package com.ht.eventbox.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.constant.Constant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    public static final String[] PUBLIC_APIS_PREFIX = {
            "/api/v1/auth/",
            "/api/v2/auth/",
            "/api/v1/orders/paypal/webhook/",
            "/actuator",
    };
    private final JwtService jwtService;

    @Value("${application.security.jwt.access-secret-key}")
    private String accessSecretKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws IOException, ServletException {

        String requestURI = request.getRequestURI();

        for (String publicApiPrefix : PUBLIC_APIS_PREFIX) {
            if (requestURI.startsWith(publicApiPrefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String sub;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response);
            return;
        }
        try {
            jwt = authHeader.substring(7);
            sub = jwtService.extractSub(jwt, accessSecretKey);

            if (sub != null) {
                List<String> roles = jwtService.extractRoles(jwt, accessSecretKey);
                List<String> permissions = jwtService.extractPermissions(jwt, accessSecretKey);
                boolean isValid = jwtService.isTokenValid(jwt, accessSecretKey);
                if (!isValid) {
                    sendUnauthorizedResponse(response);
                } else {
                    request.setAttribute("roles", roles);
                    request.setAttribute("permissions", permissions);
                    request.setAttribute("sub", sub);
                    filterChain.doFilter(request, response);
                }
                return;
            }
            sendUnauthorizedResponse(response);
        } catch (Exception e) {
            sendUnauthorizedResponse(response, e.getMessage());
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        String errors = objectMapper.writeValueAsString(Map.of(
                "code", HttpServletResponse.SC_UNAUTHORIZED,
                "message", Constant.ErrorCode.UNAUTHORIZED
        ));
        response.getWriter().write(errors);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        String errors = objectMapper.writeValueAsString(Map.of(
                "code", HttpServletResponse.SC_UNAUTHORIZED,
                "message", message
        ));
        response.getWriter().write(errors);
    }
}