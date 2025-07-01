package com.ht.eventbox.modules.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper; // Jackson ObjectMapper

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setValue(String key, String value, long expirationInSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expirationInSeconds));
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean deleteValue(String key) {
        return redisTemplate.delete(key);
    }

    public <T> void setObject(String key, T object, long expirationInSeconds) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(object);
        redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(expirationInSeconds));
    }

    public <T> T getObject(String key, Class<T> clazz) throws JsonProcessingException {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }
}