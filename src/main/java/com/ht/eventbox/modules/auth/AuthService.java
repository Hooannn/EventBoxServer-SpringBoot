package com.ht.eventbox.modules.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.FCMToken;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.dtos.*;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.redis.RedisService;
import com.ht.eventbox.modules.user.FCMTokenRepository;
import com.ht.eventbox.modules.user.RoleRepository;
import com.ht.eventbox.modules.user.UserRepository;
import com.ht.eventbox.utils.Helper;
import jakarta.mail.MessagingException;
import lombok.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleUserInfo {
        private String sub;
        private String name;
        @JsonProperty("given_name")
        private String givenName;
        @JsonProperty("family_name")
        private String familyName;
        private String picture;
        private String email;
        @JsonProperty("email_verified")
        private boolean emailVerified;
        private String locale;

        @Override
        public String toString() {
            return "GoogleUserInfo{" +
                    "sub='" + sub + '\'' +
                    ", name='" + name + '\'' +
                    ", givenName='" + givenName + '\'' +
                    ", familyName='" + familyName + '\'' +
                    ", picture='" + picture + '\'' +
                    ", email='" + email + '\'' +
                    ", emailVerified=" + emailVerified +
                    ", locale='" + locale + '\'' +
                    '}';
        }
    }

    @Getter
    @Setter
    @Builder
    public static class RegisterData {
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
        private String password;
        private String otp;
    }

    @Getter
    @Setter
    @Builder
    public static class Credentials {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("refresh_token")
        private String refreshToken;
    }

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    @Value("${application.security.jwt.refresh.expiration}")
    private long refreshExpiration;
    @Value("${application.security.jwt.password.expiration}")
    private long passwordExpiration;
    @Value("${application.security.jwt.refresh-secret-key}")
    private String refreshSecretKey;

    private final MailService mailService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final FCMTokenRepository fcmTokenRepository;

    public Credentials getCredentials(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        redisService.setValue(
                String.format("%s:user_id:%s", Constant.RedisPrefix.REFRESH_TOKEN,
                        user.getId().toString()),
                        refreshToken,
                refreshExpiration / 1000);

        return Credentials.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public boolean logout(Long userId, LogoutDto logoutDto) {
        redisService.deleteValue(String.format("%s:user_id:%s", Constant.RedisPrefix.REFRESH_TOKEN, userId.toString()));

        FCMToken fcmToken = fcmTokenRepository
                .findByUserId(userId)
                .orElse(null);

        if (fcmToken != null) {
            switch (logoutDto.getPlatform()) {
                case ANDROID -> fcmToken.setAndroid(null);
                case IOS -> fcmToken.setIos(null);
                case WEB -> fcmToken.setWebPush(null);
                default -> throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.BAD_REQUEST);
            }
            fcmTokenRepository.save(fcmToken);
        }

        return true;
    }

    public boolean register(RegisterDto registerDto) {
        if (userRepository.existsByEmail(registerDto.getUsername())) {
            throw new HttpException(Constant.ErrorCode.USER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        String otp = Helper.generateOTP();
        String hashedOtp = passwordEncoder.encode(otp);
        String hashedPassword = passwordEncoder.encode(registerDto.getPassword());

        var registerData = RegisterData.builder()
                .firstName(registerDto.getFirstName())
                .lastName(registerDto.getLastName())
                .password(hashedPassword)
                .otp(hashedOtp)
                .build();

        try {
            redisService.setObject(
                    String.format("%s:username:%s", Constant.RedisPrefix.REGISTER, registerDto.getUsername()),
                    registerData,
                    passwordExpiration / 1000
            );
        } catch (JsonProcessingException e) {
            throw new HttpException(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendRegistrationEmail(
                        registerDto.getUsername(),
                        registerDto.getFirstName() + " " + registerDto.getLastName(),
                        otp
                );
            } catch (MessagingException e) {
                logger.error("mailService.sendRegistrationEmail: {}", e.getMessage());
            }
        });

        return true;
    }

    public boolean isPasswordMatch(Long userId, String password) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return passwordEncoder.matches(password, user.getPassword());
    }

    public AuthenticationResponse authenticate(AuthenticateDto authenticateDto) {
        var user = userRepository.findByEmail(authenticateDto.getUsername())
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST));

        if (!passwordEncoder.matches(authenticateDto.getPassword(), user.getPassword())) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        var credentials = getCredentials(user);

        return AuthenticationResponse.builder()
                .user(user)
                .accessToken(credentials.getAccessToken())
                .refreshToken(credentials.getRefreshToken())
                .build();
    }

    public AuthenticationResponse googleAuthenticate(GoogleAuthenticateDto googleAuthenticateDto) {
        var googleUser = getGoogleUser(googleAuthenticateDto.getAccessToken());
        if (googleUser == null || !googleUser.isEmailVerified()) {
            throw new HttpException(Constant.ErrorCode.INVALID_TOKEN, HttpStatus.FORBIDDEN);
        }

        var user = userRepository.findByEmail(googleUser.getEmail()).orElse(null);

        if (user != null) {
            var credentials = getCredentials(user);
            return AuthenticationResponse.builder()
                    .user(user)
                    .accessToken(credentials.getAccessToken())
                    .refreshToken(credentials.getRefreshToken())
                    .build();
        }

        var defaultRole = roleRepository.findByName(Constant.DefaultRole.USER)
                .orElseThrow(() -> new HttpException("Default user role not found in database", HttpStatus.INTERNAL_SERVER_ERROR));

        var newUser = User.builder()
                .email(googleUser.getEmail())
                .firstName(googleUser.getGivenName())
                .lastName(googleUser.getFamilyName())
                .password(Helper.generateRandomSecret(12))
                .activatedAt(Helper.getCurrentDateTime())
                .roles(
                        new java.util.HashSet<>(java.util.List.of(defaultRole))
                )
                .build();

        var savedUser = userRepository.save(newUser);

        var credentials = getCredentials(savedUser);

        return AuthenticationResponse.builder()
                .user(savedUser)
                .accessToken(credentials.getAccessToken())
                .refreshToken(credentials.getRefreshToken())
                .build();
    }

    public boolean resendVerify(ResendVerifyDto resendVerifyDto) {
        if (userRepository.existsByEmail(resendVerifyDto.getUsername())) {
            throw new HttpException(Constant.ErrorCode.USER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        String otp = Helper.generateOTP();
        String hashedOtp = passwordEncoder.encode(otp);

        RegisterData registerData = null;
        try {
            registerData = redisService.getObject(
                    String.format("%s:username:%s", Constant.RedisPrefix.REGISTER, resendVerifyDto.getUsername()),
                    RegisterData.class
            );
        } catch (JsonProcessingException e) {
            throw new HttpException(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (registerData == null) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        registerData.setOtp(hashedOtp);

        try {
            redisService.setObject(
                    String.format("%s:username:%s", Constant.RedisPrefix.REGISTER, resendVerifyDto.getUsername()),
                    registerData,
                    passwordExpiration / 1000
            );
        } catch (JsonProcessingException e) {
            throw new HttpException(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        RegisterData finalRegisterData = registerData;
        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendRegistrationEmail(
                        resendVerifyDto.getUsername(),
                        finalRegisterData.getFirstName() + " " + finalRegisterData.getLastName(),
                        otp
                );
            } catch (MessagingException e) {
                logger.error("mailService.resendVerify: {}", e.getMessage());
            }
        });

        return true;
    }

    public AuthenticationResponse verify(VerifyDto verifyDto) {
        if (userRepository.existsByEmail(verifyDto.getUsername())) {
            throw new HttpException(Constant.ErrorCode.USER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        RegisterData registerData = null;
        try {
            registerData = redisService.getObject(
                    String.format("%s:username:%s", Constant.RedisPrefix.REGISTER, verifyDto.getUsername()),
                    RegisterData.class
            );
        } catch (JsonProcessingException e) {
            throw new HttpException(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (registerData == null) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(verifyDto.getOtp(), registerData.getOtp())) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        var defaultRole = roleRepository.findByName(Constant.DefaultRole.USER)
                .orElseThrow(() -> new HttpException("Default user role not found in database", HttpStatus.INTERNAL_SERVER_ERROR));

        var user = User.builder()
                .email(verifyDto.getUsername())
                .firstName(registerData.getFirstName())
                .lastName(registerData.getLastName())
                .password(registerData.getPassword())
                .roles(
                        new java.util.HashSet<>(java.util.List.of(defaultRole))
                )
                .activatedAt(Helper.getCurrentDateTime())
                .build();

        var savedUser = userRepository.save(user);

        redisService.deleteValue(String.format("%s:username:%s", Constant.RedisPrefix.REGISTER, verifyDto.getUsername()));

        var credentials = getCredentials(savedUser);

        return AuthenticationResponse.builder()
                .user(savedUser)
                .accessToken(credentials.getAccessToken())
                .refreshToken(credentials.getRefreshToken())
                .build();
    }

    public boolean forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        if (!userRepository.existsByEmail(forgotPasswordDto.getUsername())) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        String otp = Helper.generateOTP();
        String hashedOtp = passwordEncoder.encode(otp);

        redisService.setValue(
                String.format("%s:username:%s", Constant.RedisPrefix.RESET_PASSWORD_OTP, forgotPasswordDto.getUsername()),
                hashedOtp,
                passwordExpiration / 1000
        );

        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendForgotPasswordEmail(
                        forgotPasswordDto.getUsername(),
                        otp
                );
            } catch (MessagingException e) {
                logger.error("mailService.sendForgotPasswordEmail: {}", e.getMessage());
            }
        });

        return true;
    }

    public boolean resetPassword(ResetPasswordDto resetPasswordDto) {
        if (!userRepository.existsByEmail(resetPasswordDto.getUsername())) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        String storedOtp = redisService.getValue(
                String.format("%s:username:%s", Constant.RedisPrefix.RESET_PASSWORD_OTP, resetPasswordDto.getUsername())
        );

        if (storedOtp == null || !passwordEncoder.matches(resetPasswordDto.getOtp(), storedOtp)) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        String hashedPassword = passwordEncoder.encode(resetPasswordDto.getPassword());

        redisService.deleteValue(
                String.format("%s:username:%s", Constant.RedisPrefix.RESET_PASSWORD_OTP, resetPasswordDto.getUsername())
        );

        var user = userRepository.findByEmail(resetPasswordDto.getUsername())
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST));

        user.setPassword(hashedPassword);
        userRepository.save(user);

        return true;
    }

    public Credentials refresh(RefreshDto refreshDto) {
        boolean isValid = jwtService.isTokenValid(
                refreshDto.getToken(),
                refreshSecretKey
        );

        if (isValid) {
            String sub = jwtService.extractSub(refreshDto.getToken(), refreshSecretKey);
            String storedToken = redisService.getValue(String.format("%s:user_id:%s", Constant.RedisPrefix.REFRESH_TOKEN, sub));
            if (storedToken != null && storedToken.equals(refreshDto.getToken())) {
                var user = userRepository
                        .findById(Long.valueOf(sub))
                        .orElseThrow(() -> new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.FORBIDDEN));
                return getCredentials(user);
            }
        }

        throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.FORBIDDEN);
    }

    private GoogleUserInfo getGoogleUser(String token) {
        WebClient webClient = WebClient
                .builder()
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("Authorization", "Bearer " + token);
                })
                .baseUrl("https://www.googleapis.com")
                .build();

        return webClient
                .get().uri("/oauth2/v3/userinfo")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            throw new HttpException(Constant.ErrorCode.INVALID_TOKEN, HttpStatus.FORBIDDEN);
                        })
                .bodyToMono(GoogleUserInfo.class)
                .block();
    }
}
