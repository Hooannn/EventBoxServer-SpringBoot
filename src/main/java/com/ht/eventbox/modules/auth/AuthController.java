package com.ht.eventbox.modules.auth;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${application.security.jwt.access-secret-key}")
    private String accessSecretKey;

    @PostMapping("/register")
    public ResponseEntity<Response<Boolean>> register(@Valid @RequestBody RegisterDto registerDto) {
        var res = authService.register(registerDto);
        return ResponseEntity.created(null).body(
                new Response<Boolean>(
                        HttpStatus.CREATED.value(),
                        Constant.SuccessCode.REGISTER_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<Response<AuthenticationResponse>> verify(@Valid @RequestBody VerifyDto verifyDto) {
        var res = authService.verify(verifyDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.VERIFY_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<Response<Boolean>> resendVerify(@Valid @RequestBody ResendVerifyDto resendVerifyDto) {
        var res = authService.resendVerify(resendVerifyDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.RESEND_VERIFY_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<Response<AuthenticationResponse>> authenticate(@Valid @RequestBody AuthenticateDto authenticateDto) {
        var res = authService.authenticate(authenticateDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.LOGIN_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Response<Boolean>> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutDto logoutDto
    ) {
        final String jwt;
        final String sub;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new HttpException(
                    Constant.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED
            );
        }

        try {
            jwt = authHeader.substring(7);
            sub = jwtService.extractSub(jwt, accessSecretKey);
        } catch (Exception e) {
            throw new HttpException(
                    Constant.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED
            );
        }

        var res = authService.logout(Long.valueOf(sub), logoutDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.LOGOUT_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/google")
    public ResponseEntity<Response<AuthenticationResponse>> googleAuthenticate(@Valid @RequestBody GoogleAuthenticateDto googleAuthenticateDto) {
        var res = authService.googleAuthenticate(googleAuthenticateDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.LOGIN_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/forgot-password/otp")
    public ResponseEntity<Response<Boolean>> forgotPassword(@Valid @RequestBody ForgotPasswordDto forgotPasswordDto) {
        var res = authService.forgotPassword(forgotPasswordDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.FORGOT_PASSWORD_OTP_SUCCESS,
                        res
                )
        );
    }

    @PostMapping("/reset-password/otp")
    public ResponseEntity<Response<Boolean>> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        var res = authService.resetPassword(resetPasswordDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.RESET_PASSWORD_OTP_SUCCESS,
                        res
                )
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<Response<AuthService.Credentials>> refresh(@Valid @RequestBody RefreshDto refreshDto) {
        var res = authService.refresh(refreshDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.REFRESH_SUCCESS,
                        res
                )
        );
    }
}
