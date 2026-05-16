package com.ht.eventbox.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.enums.Platform;
import com.ht.eventbox.modules.auth.dtos.AuthenticateDto;
import com.ht.eventbox.modules.auth.dtos.ForgotPasswordDto;
import com.ht.eventbox.modules.auth.dtos.GoogleAuthenticateDto;
import com.ht.eventbox.modules.auth.dtos.GoogleAuthenticateWithIdTokenDto;
import com.ht.eventbox.modules.auth.dtos.LogoutDto;
import com.ht.eventbox.modules.auth.dtos.RefreshDto;
import com.ht.eventbox.modules.auth.dtos.RegisterDto;
import com.ht.eventbox.modules.auth.dtos.ResendVerifyDto;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.utils.CookieUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.PublicKey;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class AuthControllerTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CookieUtil cookieUtil;

        @MockBean
        private AuthService authService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private PublicKey atPublicKey;

        @Test
        void register_shouldReturnCreatedResponse() throws Exception {
                when(authService.register(any(RegisterDto.class))).thenReturn(true);

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                RegisterDto.builder()
                                                                .username("user@example.com")
                                                                .password("password")
                                                                .firstName("Test")
                                                                .lastName("User")
                                                                .build())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.code").value(201))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.REGISTER_SUCCESS))
                                .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        void verify_shouldSetCookiesAndReturnTokenPayload() throws Exception {
                var response = AuthService.Credentials.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .build();
                var user = sampleUser();
                var authResponse = AuthenticationResponse.builder()
                                .user(user)
                                .accessToken(response.getAccessToken())
                                .refreshToken(response.getRefreshToken())
                                .build();

                when(authService.verify(any())).thenReturn(authResponse);
                when(cookieUtil.createAccessTokenCookie("access-token"))
                                .thenReturn(cookie(Constant.ContextKey.ACCESS_TOKEN, "access-token", "/api"));
                when(cookieUtil.createRefreshTokenCookie("refresh-token"))
                                .thenReturn(cookie(Constant.ContextKey.REFRESH_TOKEN, "refresh-token",
                                                "/api/v1/auth/refresh"));

                mockMvc.perform(post("/api/v1/auth/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                com.ht.eventbox.modules.auth.dtos.VerifyDto.builder()
                                                                .username("user@example.com")
                                                                .otp("123456")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.VERIFY_SUCCESS))
                                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"))
                                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                                .andExpect(result -> {
                                        var cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                                        assertThat(cookies).hasSize(2);
                                        assertThat(cookies).anyMatch(value -> value
                                                        .contains(Constant.ContextKey.ACCESS_TOKEN + "=access-token"));
                                        assertThat(cookies).anyMatch(value -> value.contains(
                                                        Constant.ContextKey.REFRESH_TOKEN + "=refresh-token"));
                                });
        }

        @Test
        void resendVerify_shouldReturnSuccessResponse() throws Exception {
                when(authService.resendVerify(any(ResendVerifyDto.class))).thenReturn(true);

                mockMvc.perform(post("/api/v1/auth/verify/resend")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                ResendVerifyDto.builder()
                                                                .username("user@example.com")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.RESEND_VERIFY_SUCCESS))
                                .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        void googleAuthenticate_shouldSetCookiesAndReturnTokenPayload() throws Exception {
                var response = AuthService.Credentials.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .build();
                var user = sampleUser();
                var authResponse = AuthenticationResponse.builder()
                                .user(user)
                                .accessToken(response.getAccessToken())
                                .refreshToken(response.getRefreshToken())
                                .build();

                when(authService.googleAuthenticate(any(GoogleAuthenticateDto.class))).thenReturn(authResponse);
                when(cookieUtil.createAccessTokenCookie("access-token"))
                                .thenReturn(cookie(Constant.ContextKey.ACCESS_TOKEN, "access-token", "/api"));
                when(cookieUtil.createRefreshTokenCookie("refresh-token"))
                                .thenReturn(cookie(Constant.ContextKey.REFRESH_TOKEN, "refresh-token",
                                                "/api/v1/auth/refresh"));

                mockMvc.perform(post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                GoogleAuthenticateDto.builder()
                                                                .accessToken("google-token")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.LOGIN_SUCCESS))
                                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"))
                                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                                .andExpect(result -> {
                                        var cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                                        assertThat(cookies).hasSize(2);
                                        assertThat(cookies).anyMatch(value -> value
                                                        .contains(Constant.ContextKey.ACCESS_TOKEN + "=access-token"));
                                        assertThat(cookies).anyMatch(value -> value.contains(
                                                        Constant.ContextKey.REFRESH_TOKEN + "=refresh-token"));
                                });
        }

        @Test
        void googleAuthenticateWithIdToken_shouldReturnTokenPayloadWithoutCookies() throws Exception {
                var response = AuthService.Credentials.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .build();
                var user = sampleUser();
                var authResponse = AuthenticationResponse.builder()
                                .user(user)
                                .accessToken(response.getAccessToken())
                                .refreshToken(response.getRefreshToken())
                                .build();

                when(authService.googleAuthenticateWithIdToken(any(GoogleAuthenticateWithIdTokenDto.class)))
                                .thenReturn(authResponse);

                mockMvc.perform(post("/api/v1/auth/google/tokeninfo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                GoogleAuthenticateWithIdTokenDto.builder()
                                                                .idToken("id-token")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.LOGIN_SUCCESS))
                                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"))
                                .andExpect(jsonPath("$.data.user.email").value("user@example.com"));
        }

        @Test
        void forgotPassword_shouldReturnSuccessResponse() throws Exception {
                when(authService.forgotPassword(any(ForgotPasswordDto.class))).thenReturn(true);

                mockMvc.perform(post("/api/v1/auth/forgot-password/otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                ForgotPasswordDto.builder()
                                                                .username("user@example.com")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.FORGOT_PASSWORD_OTP_SUCCESS))
                                .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        void resetPassword_shouldReturnSuccessResponse() throws Exception {
                when(authService.resetPassword(any())).thenReturn(true);

                mockMvc.perform(post("/api/v1/auth/reset-password/otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                com.ht.eventbox.modules.auth.dtos.ResetPasswordDto.builder()
                                                                .username("user@example.com")
                                                                .password("new-password")
                                                                .otp("123456")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.RESET_PASSWORD_OTP_SUCCESS))
                                .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        void logout_shouldClearCookiesAndReturnSuccessResponse() throws Exception {
                when(jwtService.extractSub("access-token", atPublicKey)).thenReturn("42");
                when(authService.logout(eq(42L), any(LogoutDto.class))).thenReturn(true);
                when(cookieUtil.cleanAccessTokenCookie())
                                .thenReturn(cookie(Constant.ContextKey.ACCESS_TOKEN, "", "/api"));
                when(cookieUtil.cleanRefreshTokenCookie())
                                .thenReturn(cookie(Constant.ContextKey.REFRESH_TOKEN, "", "/api/v1/auth/refresh"));

                mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer access-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                LogoutDto.builder()
                                                                .platform(Platform.WEB)
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.LOGOUT_SUCCESS))
                                .andExpect(jsonPath("$.data").value(true))
                                .andExpect(result -> {
                                        var cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                                        assertThat(cookies).hasSize(2);
                                });
        }

        @Test
        void logout_shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
                mockMvc.perform(post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                LogoutDto.builder()
                                                                .platform(Platform.WEB)
                                                                .build())))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(401))
                                .andExpect(jsonPath("$.message").value(Constant.ErrorCode.UNAUTHORIZED));
        }

        @Test
        void login_shouldSetCookiesAndReturnTokenPayload() throws Exception {
                var response = AuthService.Credentials.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .build();
                var user = sampleUser();
                var authResponse = AuthenticationResponse.builder()
                                .user(user)
                                .accessToken(response.getAccessToken())
                                .refreshToken(response.getRefreshToken())
                                .build();

                when(authService.authenticate(any(AuthenticateDto.class))).thenReturn(authResponse);
                when(cookieUtil.createAccessTokenCookie("access-token"))
                                .thenReturn(cookie(Constant.ContextKey.ACCESS_TOKEN, "access-token", "/api"));
                when(cookieUtil.createRefreshTokenCookie("refresh-token"))
                                .thenReturn(cookie(Constant.ContextKey.REFRESH_TOKEN, "refresh-token",
                                                "/api/v1/auth/refresh"));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                AuthenticateDto.builder()
                                                                .username("user@example.com")
                                                                .password("password")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.LOGIN_SUCCESS))
                                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"))
                                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                                .andExpect(result -> {
                                        var cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                                        assertThat(cookies).hasSize(2);
                                        assertThat(cookies).anyMatch(value -> value
                                                        .contains(Constant.ContextKey.ACCESS_TOKEN + "=access-token"));
                                        assertThat(cookies).anyMatch(value -> value.contains(
                                                        Constant.ContextKey.REFRESH_TOKEN + "=refresh-token"));
                                });
        }

        @Test
        void refresh_shouldUseRefreshTokenFromCookie() throws Exception {
                var response = AuthService.Credentials.builder()
                                .accessToken("new-access-token")
                                .refreshToken("new-refresh-token")
                                .build();

                when(authService.refresh(any(RefreshDto.class))).thenReturn(response);
                when(cookieUtil.createAccessTokenCookie("new-access-token"))
                                .thenReturn(cookie(Constant.ContextKey.ACCESS_TOKEN, "new-access-token", "/api"));
                when(cookieUtil.createRefreshTokenCookie("new-refresh-token"))
                                .thenReturn(cookie(Constant.ContextKey.REFRESH_TOKEN, "new-refresh-token",
                                                "/api/v1/auth/refresh"));

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .cookie(new Cookie(Constant.ContextKey.REFRESH_TOKEN, "cookie-refresh-token")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.REFRESH_SUCCESS))
                                .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                                .andExpect(jsonPath("$.data.refresh_token").value("new-refresh-token"));

                verify(authService).refresh(argThat(refreshDto -> refreshDto != null
                                && "cookie-refresh-token".equals(refreshDto.getToken())));
        }

        @Test
        void refresh_shouldUseTokenFromBodyWhenProvided() throws Exception {
                var response = AuthService.Credentials.builder()
                                .accessToken("new-access-token")
                                .refreshToken("new-refresh-token")
                                .build();

                when(authService.refresh(any(RefreshDto.class))).thenReturn(response);
                when(cookieUtil.createAccessTokenCookie("new-access-token"))
                                .thenReturn(cookie(Constant.ContextKey.ACCESS_TOKEN, "new-access-token", "/api"));
                when(cookieUtil.createRefreshTokenCookie("new-refresh-token"))
                                .thenReturn(cookie(Constant.ContextKey.REFRESH_TOKEN, "new-refresh-token",
                                                "/api/v1/auth/refresh"));

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                RefreshDto.builder()
                                                                .token("body-refresh-token")
                                                                .build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.REFRESH_SUCCESS))
                                .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                                .andExpect(jsonPath("$.data.refresh_token").value("new-refresh-token"));

                verify(authService).refresh(argThat(refreshDto -> refreshDto != null
                                && "body-refresh-token".equals(refreshDto.getToken())));
        }

        @Test
        void refresh_shouldReturnBadRequestWhenTokenMissing() throws Exception {
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400))
                                .andExpect(jsonPath("$.message").value(Constant.ErrorCode.INVALID_TOKEN));
        }

        private User sampleUser() {
                return User.builder()
                                .id(42L)
                                .email("user@example.com")
                                .firstName("Test")
                                .lastName("User")
                                .build();
        }

        private ResponseCookie cookie(String name, String value, String path) {
                return ResponseCookie.from(name, value)
                                .path(path)
                                .httpOnly(true)
                                .build();
        }
}
