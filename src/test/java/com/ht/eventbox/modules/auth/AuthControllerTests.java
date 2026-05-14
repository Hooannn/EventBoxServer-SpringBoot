package com.ht.eventbox.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.dtos.AuthenticateDto;
import com.ht.eventbox.modules.auth.dtos.RefreshDto;
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
