# Global Per-IP Rate Limit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce a Redis-backed global per-IP rate limit of 1000 requests per minute across all application instances.

**Architecture:** Add a dedicated servlet filter that resolves the client IP, checks a Redis counter for the current minute window, and rejects requests with HTTP 429 once the limit is exceeded. Register that filter before `AuthenticationFilter` so abusive traffic is blocked before JWT work happens. Reuse the existing `RedisService` for atomic counter increments and TTL management.

**Tech Stack:** Java 17, Spring Boot, Spring Web filters, Spring Data Redis, MockMvc, JUnit 5, Mockito

---

### Task 1: Add Redis-backed limiter primitives and request filter

**Files:**

- Modify: `src/main/java/com/ht/eventbox/modules/redis/RedisService.java`
- Create: `src/main/java/com/ht/eventbox/filter/RateLimiterFilter.java`
- Modify: `src/main/java/com/ht/eventbox/config/RateLimiterConfig.java`
- Modify: `src/main/java/com/ht/eventbox/config/FilterConfiguration.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-dev.properties`
- Modify: `src/test/resources/application-test.properties`

- [ ] **Step 1: Add the Redis increment helper**

```java
public Long incrementValue(String key) {
    return redisTemplate.opsForValue().increment(key);
}

public Boolean expireKey(String key, long expirationInSeconds) {
    return redisTemplate.expire(key, Duration.ofSeconds(expirationInSeconds));
}
```

- [ ] **Step 2: Implement the filter**

```java
@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {
    private final RedisService redisService;

    @Value("${application.rate-limit.requests-per-minute:1000}")
    private long requestsPerMinute;

    @Value("${application.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String clientIp = resolveClientIp(request);
        String windowKey = buildWindowKey(clientIp);
        Long currentCount = redisService.incrementValue(windowKey);
        if (currentCount != null && currentCount == 1L) {
            redisService.expireKey(windowKey, windowSeconds);
        }
        if (currentCount != null && currentCount > requestsPerMinute) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                ExceptionResponse.builder().code(429).message("rate_limit_exceeded").build()
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Register the rate limiter before authentication**

```java
@Bean
public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilterRegistrationBean(RateLimiterFilter rateLimiterFilter) {
    FilterRegistrationBean<RateLimiterFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(rateLimiterFilter);
    registrationBean.addUrlPatterns("/*");
    registrationBean.setOrder(0);
    return registrationBean;
}
```

```java
@Bean
public FilterRegistrationBean<AuthenticationFilter> authenticationFilterFilterRegistrationBean() {
    FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(authenticationFilter);
    registrationBean.addUrlPatterns("/*");
    registrationBean.setOrder(1);
    return registrationBean;
}
```

- [ ] **Step 4: Add config properties for the 1000/minute policy**

```properties
application.rate-limit.requests-per-minute=1000
application.rate-limit.window-seconds=60
```

### Task 2: Add coverage for the limiter behavior

**Files:**

- Create: `src/test/java/com/ht/eventbox/filter/RateLimiterFilterTests.java`
- Modify: `src/test/resources/application-test.properties`

- [ ] **Step 1: Write a test that allows requests below the limit**

```java
class RateLimiterFilterTests {
    @Autowired private RateLimiterFilter rateLimiterFilter;
    @MockBean private RedisService redisService;

    @Test
    void shouldAllowRequestsUnderLimit() throws Exception {
        when(redisService.incrementValue(anyString())).thenReturn(1L);
        var request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.setRemoteAddr("203.0.113.10");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        rateLimiterFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
```

- [ ] **Step 2: Write a test that blocks requests above the limit**

```java
@Test
void shouldReturn429WhenLimitExceeded() throws Exception {
    when(redisService.incrementValue(anyString())).thenReturn(1001L);
    var request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
    request.setRemoteAddr("203.0.113.10");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    rateLimiterFilter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getContentAsString()).contains("\"message\":\"rate_limit_exceeded\"");
}
```

- [ ] **Step 3: Run the rate limiter tests**

Run: `./mvnw -Dtest=RateLimiterFilterTests test`
Expected: PASS

- [ ] **Step 4: Run the full test suite**

Run: `./mvnw test`
Expected: PASS
