package com.ht.eventbox.modules.auth;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.dtos.*;
import com.ht.eventbox.utils.CookieUtil;
import com.ht.eventbox.utils.Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CookieUtil cookieUtil;
    private final AuthService authService;
    private final JwtService jwtService;
    private final PublicKey atPublicKey;

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

        ResponseCookie atCookie = cookieUtil.createAccessTokenCookie(res.getAccessToken());
        ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(res.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                .body(new Response<>(
                                HttpStatus.OK.value(),
                                Constant.SuccessCode.VERIFY_SUCCESS,
                                res
                ));
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

        ResponseCookie atCookie = cookieUtil.createAccessTokenCookie(res.getAccessToken());
        ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(res.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                .body(new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.LOGIN_SUCCESS,
                        res
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Response<Boolean>> logout(
            @NonNull HttpServletRequest request,
            @Valid @RequestBody LogoutDto logoutDto
    ) {
        String jwt = Request.getTokenFromHeader(request)
                .or(() -> Request.getTokenFromCookie(request))
                .orElse(null);

        if (jwt == null) {
            throw new HttpException(
                    Constant.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED
            );
        }

        try {
            String sub = jwtService.extractSub(jwt, atPublicKey);
            var res = authService.logout(Long.valueOf(sub), logoutDto);

            ResponseCookie atCookie = cookieUtil.cleanAccessTokenCookie();
            ResponseCookie rtCookie = cookieUtil.cleanRefreshTokenCookie();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                    .body(new Response<>(
                            HttpStatus.OK.value(),
                            Constant.SuccessCode.LOGOUT_SUCCESS,
                            res
                    ));
        } catch (Exception e) {
            throw new HttpException(
                    Constant.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @PostMapping("/google")
    public ResponseEntity<Response<AuthenticationResponse>> googleAuthenticate(@Valid @RequestBody GoogleAuthenticateDto googleAuthenticateDto) {
        var res = authService.googleAuthenticate(googleAuthenticateDto);

        ResponseCookie atCookie = cookieUtil.createAccessTokenCookie(res.getAccessToken());
        ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(res.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                .body(new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.LOGIN_SUCCESS,
                        res
                ));
    }

    @PostMapping("/google/tokeninfo")
    public ResponseEntity<Response<AuthenticationResponse>> googleAuthenticateWithIdToken(@Valid @RequestBody GoogleAuthenticateWithIdTokenDto googleAuthenticateDto) {
        var res = authService.googleAuthenticateWithIdToken(googleAuthenticateDto);
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

        ResponseCookie atCookie = cookieUtil.createAccessTokenCookie(res.getAccessToken());
        ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(res.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString())
                .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                .body(new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.REFRESH_SUCCESS,
                        res
                ));
    }
}
