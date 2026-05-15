package com.ht.eventbox.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.modules.redis.RedisService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTests {

    @Mock
    private RedisService redisService;

    private RateLimiterFilter rateLimiterFilter;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T22:45:00Z"), ZoneOffset.UTC);
        rateLimiterFilter = new RateLimiterFilter(redisService, new ObjectMapper(), fixedClock);
        ReflectionTestUtils.setField(rateLimiterFilter, "requestsPerMinute", 1000L);
        ReflectionTestUtils.setField(rateLimiterFilter, "windowSeconds", 60L);
    }

    @Test
    void shouldAllowRequestsUnderLimitAndSetExpiryForFirstHit() throws Exception {
        when(redisService.incrementValue(startsWith("rate_limit:ip:203.0.113.10:"))).thenReturn(1L);
        when(redisService.expireKey(startsWith("rate_limit:ip:203.0.113.10:"), anyLong())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        rateLimiterFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("RateLimit-Limit")).isEqualTo("1000");
        assertThat(response.getHeader("RateLimit-Remaining")).isEqualTo("999");
        assertThat(response.getHeader("RateLimit-Reset")).isEqualTo("60");
        assertThat(response.getHeader("RateLimit-Policy")).isEqualTo("1000;w=60");
        verify(chain).doFilter(request, response);
        verify(redisService).expireKey(startsWith("rate_limit:ip:203.0.113.10:"), anyLong());
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        when(redisService.incrementValue(startsWith("rate_limit:ip:198.51.100.7:"))).thenReturn(1001L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.setRemoteAddr("198.51.100.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        rateLimiterFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("RateLimit-Limit")).isEqualTo("1000");
        assertThat(response.getHeader("RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("RateLimit-Reset")).isEqualTo("60");
        assertThat(response.getHeader("RateLimit-Policy")).isEqualTo("1000;w=60");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("\"message\":\"rate_limit_exceeded\"");
        verifyNoInteractions(chain);
    }
}
