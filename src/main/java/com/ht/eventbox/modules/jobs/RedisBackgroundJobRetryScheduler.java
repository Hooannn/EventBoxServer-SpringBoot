package com.ht.eventbox.modules.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisBackgroundJobRetryScheduler {
    static final String RETRY_KEY = "jobs:retry";
    static final String DEAD_KEY = "jobs:dead";
    static final String META_PREFIX = "jobs:meta:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.jobs.max-attempts:3}")
    private int maxAttempts;

    @Value("${application.jobs.initial-backoff-seconds:10}")
    private long initialBackoffSeconds;

    public boolean shouldRetry(BackgroundJobEnvelope envelope) {
        return envelope.attempt() + 1 < maxAttempts;
    }

    public void scheduleRetry(BackgroundJobEnvelope envelope, Exception exception) {
        int nextAttempt = envelope.attempt() + 1;
        Instant runAfter = Instant.now().plus(backoffForAttempt(nextAttempt));
        BackgroundJobEnvelope retryEnvelope = envelope.withAttempt(nextAttempt, runAfter);

        redisTemplate.opsForZSet().add(RETRY_KEY, serialize(retryEnvelope), runAfter.toEpochMilli());
        writeMeta(retryEnvelope, "RETRY_SCHEDULED", exception.getMessage());
    }

    public void moveToDeadLetter(BackgroundJobEnvelope envelope, Exception exception) {
        redisTemplate.opsForList().leftPush(DEAD_KEY, serialize(envelope));
        writeMeta(envelope, "DEAD_LETTER", exception.getMessage());
    }

    Duration backoffForAttempt(int attempt) {
        long multiplier = 1L << Math.max(0, attempt - 1);
        return Duration.ofSeconds(initialBackoffSeconds * multiplier);
    }

    private void writeMeta(BackgroundJobEnvelope envelope, String status, String lastError) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("jobId", envelope.jobId());
            meta.put("type", envelope.type().name());
            meta.put("status", status);
            meta.put("attempt", envelope.attempt());
            meta.put("payload", envelope.payload());
            meta.put("createdAt", envelope.createdAt().toString());
            meta.put("runAfter", envelope.runAfter().toString());
            if (envelope.correlationId() != null) {
                meta.put("correlationId", envelope.correlationId());
            }
            meta.put("lastError", lastError);
            redisTemplate.opsForValue().set(META_PREFIX + envelope.jobId(), objectMapper.writeValueAsString(meta));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize job metadata", e);
        }
    }

    private String serialize(BackgroundJobEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize job envelope", e);
        }
    }
}
