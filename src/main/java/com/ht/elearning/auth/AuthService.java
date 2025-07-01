package com.ht.elearning.auth;

import com.ht.elearning.auth.dtos.*;
import com.ht.elearning.config.HttpException;
import com.ht.elearning.jwt.JwtService;
import com.ht.elearning.redis.RedisService;
import com.ht.elearning.user.Role;
import com.ht.elearning.user.User;
import com.ht.elearning.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    private final AuthProcessor authProcessor;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;

    public AuthenticationResponse register(RegisterDto registerDto) {
        try {
            if (userRepository.existsByEmail(registerDto.getEmail())) {
                throw new HttpException("Email is already registered", HttpStatus.BAD_REQUEST);
            }
            var user = User.builder()
                    .firstName(registerDto.getFirstName())
                    .lastName(registerDto.getLastName())
                    .email(registerDto.getEmail())
                    .password(passwordEncoder.encode(registerDto.getPassword()))
                    .role(Role.USER)
                    .build();
            var savedUser = userRepository.save(user);
            authProcessor.processAccountVerification(savedUser);
            return AuthenticationResponse
                    .builder()
                    .credentials(getCredentials(savedUser))
                    .user(savedUser)
                    .build();
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public AuthenticationResponse authenticate(AuthenticateDto authenticateDto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticateDto.getEmail(),
                            authenticateDto.getPassword()
                    )
            );
            var user = userRepository.findByEmail(authenticateDto.getEmail()).orElseThrow();
            return AuthenticationResponse
                    .builder()
                    .credentials(getCredentials(user))
                    .user(user)
                    .build();
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (AuthenticationException e) {
            throw new HttpException(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }


    public boolean verifyAccount(VerifyAccountDto verifyAccountDto) {
        var signature = verifyAccountDto.getSignature();
        var email = verifyAccountDto.getEmail();
        var user = userRepository.findByEmail(email).orElseThrow(() -> new HttpException(
                "Invalid signature",
                HttpStatus.FORBIDDEN
        ));
        if (user.isVerified()) {
            throw new HttpException("Request is not acceptable", HttpStatus.NOT_ACCEPTABLE);
        }
        var validSignature = redisService.getValue("account_signature:" + email);
        if (signature.equals(validSignature)) {
            user.setVerified(true);
            userRepository.save(user);
            try {
                authProcessor.processWelcomeUser(user);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return true;
        }
        throw new HttpException("Invalid signature", HttpStatus.FORBIDDEN);
    }

    public boolean forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        try {
            var email = forgotPasswordDto.getEmail();
            boolean exists = userRepository.existsByEmail(email);
            if (!exists) throw new HttpException("Bad request", HttpStatus.BAD_REQUEST);
            authProcessor.processForgotPassword(email);
            return true;
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public boolean resetPassword(ResetPasswordDto resetPasswordDto) {
        try {
            var validSignature = redisService.getValue("reset_password_signature:" + resetPasswordDto.getEmail());
            var signature = resetPasswordDto.getSignature();
            if (signature.equals(validSignature)) {
                var user = userRepository.findByEmail(resetPasswordDto.getEmail()).orElseThrow();
                user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
                userRepository.save(user);
                redisService.deleteValue("refresh_token:" + user.getId());
                redisService.deleteValue("reset_password_signature:" + user.getEmail());
                return true;
            }
            throw new HttpException("Bad credentials", HttpStatus.FORBIDDEN);
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (AuthenticationException e) {
            throw new HttpException(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    public Credentials refresh(RefreshDto refreshDto) {
        try {
            final String ERROR_MESSAGE = "Bad credentials";
            var token = refreshDto.getToken();
            var isValidToken = jwtService.isTokenValid(token, true);
            if (isValidToken) {
                String sub = jwtService.extractSub(token, true);
                String storedToken = redisService.getValue("refresh_token:" + sub);
                if (storedToken.equals(token)) {
                    var user = userRepository
                            .findById(sub)
                            .orElseThrow(() -> new HttpException(ERROR_MESSAGE, HttpStatus.FORBIDDEN));
                    return getCredentials(user);
                }
            }
            throw new HttpException(ERROR_MESSAGE, HttpStatus.FORBIDDEN);
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    private Credentials getCredentials(User user) {
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        redisService.setValue("refresh_token:" + user.getId(), refreshToken, refreshExpiration / 1000);
        return Credentials
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
