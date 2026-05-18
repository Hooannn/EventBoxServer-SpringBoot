package com.ht.eventbox.modules.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisBackgroundJobService {
    static final String STREAM_KEY = "jobs:stream";
    static final String META_PREFIX = "jobs:meta:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public BackgroundJobEnvelope enqueueSendMail(MailKind mailKind, Map<String, String> payload) {
        return enqueueSendMail(mailKind, payload, null);
    }

    public BackgroundJobEnvelope enqueueSendMail(MailKind mailKind, Map<String, String> payload, String correlationId) {
        Map<String, String> jobPayload = new LinkedHashMap<>(payload);
        jobPayload.put("mailKind", mailKind.name());
        return enqueue(BackgroundJobType.SEND_MAIL, jobPayload, correlationId);
    }

    public BackgroundJobEnvelope enqueueSendPushNotification(Map<String, String> payload) {
        return enqueueSendPushNotification(payload, null);
    }

    public BackgroundJobEnvelope enqueueSendPushNotification(Map<String, String> payload, String correlationId) {
        return enqueue(BackgroundJobType.SEND_PUSH_NOTIFICATION, payload, correlationId);
    }

    BackgroundJobEnvelope enqueue(BackgroundJobType type, Map<String, String> payload, String correlationId) {
        var envelope = new BackgroundJobEnvelope(
                UUID.randomUUID().toString(),
                type,
                serializePayload(payload),
                0,
                Instant.now(),
                Instant.now(),
                correlationId);

        Map<String, String> streamEntry = new LinkedHashMap<>();
        streamEntry.put("jobId", envelope.jobId());
        streamEntry.put("type", envelope.type().name());
        streamEntry.put("payload", envelope.payload());
        streamEntry.put("attempt", String.valueOf(envelope.attempt()));
        streamEntry.put("createdAt", envelope.createdAt().toString());
        streamEntry.put("runAfter", envelope.runAfter().toString());
        if (correlationId != null && !correlationId.isBlank()) {
            streamEntry.put("correlationId", correlationId);
        }

        redisTemplate.opsForStream().add(StreamRecords.string(streamEntry).withStreamKey(STREAM_KEY));
        writeMeta(envelope, "PENDING");
        return envelope;
    }

    private void writeMeta(BackgroundJobEnvelope envelope, String status) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
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
            valueOperations.set(META_PREFIX + envelope.jobId(), objectMapper.writeValueAsString(meta));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize job metadata", e);
        }
    }

    private String serializePayload(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize job payload", e);
        }
    }
}
