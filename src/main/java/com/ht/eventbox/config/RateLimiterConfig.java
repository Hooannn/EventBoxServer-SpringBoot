package com.ht.eventbox.config;

import com.ht.eventbox.filter.RateLimiterFilter;
import com.ht.eventbox.modules.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RateLimiterConfig {
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    public RateLimiterFilter rateLimiterFilter(RedisService redisService, ObjectMapper objectMapper, Clock systemClock) {
        return new RateLimiterFilter(redisService, objectMapper, systemClock);
    }

    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilterRegistrationBean(RateLimiterFilter rateLimiterFilter) {
        FilterRegistrationBean<RateLimiterFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(rateLimiterFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(0);
        return registrationBean;
    }
}
