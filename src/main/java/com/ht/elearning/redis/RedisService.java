package com.ht.elearning.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;
    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Boolean deleteValue(String key) {
        return redisTemplate.delete(key);
    }
    public void setValue(String key, String value, long expirationInSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expirationInSeconds));
    }
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
