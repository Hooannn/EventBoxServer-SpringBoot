package com.ht.eventbox.utils;

import com.ht.eventbox.constant.Constant;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public class Request {
    public static Optional<String> getTokenFromHeader(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }

        return Optional.empty();
    }

    public static Optional<String> getTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (Constant.ContextKey.ACCESS_TOKEN.equals(cookie.getName())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }

        return Optional.empty();
    }
}
