package com.ht.eventbox.filter;

import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.config.ExceptionResponse;
import com.ht.eventbox.modules.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${application.rate-limit.requests-per-minute:1000}")
    private long requestsPerMinute;

    @Value("${application.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws IOException, ServletException {
        String clientIp = resolveClientIp(request);
        String windowKey = buildWindowKey(clientIp);
        Long currentCount = redisService.incrementValue(windowKey);
        long resetSeconds = calculateResetSeconds();
        long remainingRequests = calculateRemainingRequests(currentCount);

        setRateLimitHeaders(response, remainingRequests, resetSeconds);

        if (currentCount != null && currentCount == 1L) {
            redisService.expireKey(windowKey, windowSeconds);
        }

        if (currentCount != null && currentCount > requestsPerMinute) {
            writeTooManyRequests(response, resetSeconds);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String buildWindowKey(String clientIp) {
        long timeBucket = Instant.now(clock).getEpochSecond() / windowSeconds;
        return String.format("%s:ip:%s:%d", Constant.RedisPrefix.RATE_LIMIT, clientIp, timeBucket);
    }

    private long calculateResetSeconds() {
        long nowEpochSeconds = Instant.now(clock).getEpochSecond();
        long windowEndEpochSeconds = ((nowEpochSeconds / windowSeconds) + 1) * windowSeconds;
        return Math.max(1L, windowEndEpochSeconds - nowEpochSeconds);
    }

    private long calculateRemainingRequests(Long currentCount) {
        if (currentCount == null) {
            return requestsPerMinute;
        }

        return Math.max(0L, requestsPerMinute - currentCount);
    }

    private void setRateLimitHeaders(HttpServletResponse response, long remainingRequests, long resetSeconds) {
        response.setHeader("RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("RateLimit-Remaining", String.valueOf(remainingRequests));
        response.setHeader("RateLimit-Reset", String.valueOf(resetSeconds));
        response.setHeader("RateLimit-Policy", String.format("%d;w=%d", requestsPerMinute, windowSeconds));
    }

    private String resolveClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(value -> value.split(",")[0].trim())
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                        .filter(value -> !value.isBlank())
                        .orElseGet(request::getRemoteAddr));
    }

    private void writeTooManyRequests(HttpServletResponse response, long resetSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(resetSeconds));
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ExceptionResponse.builder()
                        .code(HttpStatus.TOO_MANY_REQUESTS.value())
                        .message(Constant.ErrorCode.RATE_LIMIT_EXCEEDED)
                        .build()
        ));
    }
}
