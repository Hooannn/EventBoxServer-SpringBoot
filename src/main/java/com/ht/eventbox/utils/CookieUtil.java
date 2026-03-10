package com.ht.eventbox.utils;

import com.ht.eventbox.constant.Constant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${application.security.jwt.expiration}")
    private long accessExpiration;

    @Value("${application.security.jwt.refresh.expiration}")
    private long refreshExpiration;

    public ResponseCookie createAccessTokenCookie(String jwt) {
        return ResponseCookie.from(Constant.ContextKey.ACCESS_TOKEN, jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessExpiration / 1000)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie cleanAccessTokenCookie() {
        return ResponseCookie.from(Constant.ContextKey.ACCESS_TOKEN, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // Xóa cookie ngay lập tức
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String jwt) {
        return ResponseCookie.from(Constant.ContextKey.REFRESH_TOKEN, jwt)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh-token") // Chỉ gửi cookie này cho endpoint refresh token
                .maxAge(refreshExpiration / 1000)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie cleanRefreshTokenCookie() {
        return ResponseCookie.from(Constant.ContextKey.REFRESH_TOKEN, "")
                .httpOnly(true)
                .path("/api/v1/auth/refresh-token") // Chỉ gửi cookie này cho endpoint refresh token
                .maxAge(0) // Xóa cookie ngay lập tức
                .build();
    }
}