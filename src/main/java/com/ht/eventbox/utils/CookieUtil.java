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

    @Value("${application.security.cookies.secure}")
    private boolean secure;

    @Value("${application.security.cookies.http-only}")
    private boolean httpOnly;

    @Value("${application.security.cookies.same-site}")
    private String sameSite;

    @Value("${application.security.cookies.refresh-token-path}")
    private String refreshTokenPath;

    @Value("${application.security.cookies.access-token-path}")
    private String accessTokenPath;

    public ResponseCookie createAccessTokenCookie(String jwt) {
        return ResponseCookie.from(Constant.ContextKey.ACCESS_TOKEN, jwt)
                .httpOnly(httpOnly)
                .secure(secure)
                .path(accessTokenPath)
                .maxAge(accessExpiration / 1000)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie cleanAccessTokenCookie() {
        return ResponseCookie.from(Constant.ContextKey.ACCESS_TOKEN, "")
                .httpOnly(httpOnly)
                .path(accessTokenPath)
                .maxAge(0) // Xóa cookie ngay lập tức
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String jwt) {
        return ResponseCookie.from(Constant.ContextKey.REFRESH_TOKEN, jwt)
                .httpOnly(httpOnly)
                .secure(secure)
                .path(refreshTokenPath) // Chỉ gửi cookie này cho endpoint refresh token
                .maxAge(refreshExpiration / 1000)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie cleanRefreshTokenCookie() {
        return ResponseCookie.from(Constant.ContextKey.REFRESH_TOKEN, "")
                .httpOnly(true)
                .path(refreshTokenPath) // Chỉ gửi cookie này cho endpoint refresh token
                .maxAge(0) // Xóa cookie ngay lập tức
                .build();
    }
}