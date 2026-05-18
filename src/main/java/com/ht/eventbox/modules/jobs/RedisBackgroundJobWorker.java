package com.ht.eventbox.modules.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RedisBackgroundJobWorker {
    static final String CONSUMER_GROUP = "jobs:consumer-group";

    private final Map<BackgroundJobType, RedisBackgroundJobHandler> handlers;
    private final RedisBackgroundJobRetryScheduler retryScheduler;
    private RedisTemplate<String, String> redisTemplate;
    private final String consumerName = UUID.randomUUID().toString();

    public RedisBackgroundJobWorker(List<RedisBackgroundJobHandler> handlerList, RedisBackgroundJobRetryScheduler retryScheduler) {
        this.handlers = new EnumMap<>(BackgroundJobType.class);
        handlerList.forEach(handler -> this.handlers.put(handler.supports(), handler));
        this.retryScheduler = retryScheduler;
    }

    @Autowired
    void configureRedis(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void processOne(BackgroundJobEnvelope envelope) {
        var handler = handlers.get(envelope.type());
        if (handler == null) {
            throw new IllegalStateException("No handler registered for job type " + envelope.type());
        }

        try {
            handler.handle(envelope);
        } catch (Exception ex) {
            if (retryScheduler.shouldRetry(envelope)) {
                retryScheduler.scheduleRetry(envelope, ex);
            } else {
                retryScheduler.moveToDeadLetter(envelope, ex);
            }
        }
    }

    @Scheduled(fixedDelayString = "${application.jobs.poll-delay-ms:1000}")
    public void pollOnce() {
        if (redisTemplate == null) {
            return;
        }

        ensureConsumerGroup();

        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(CONSUMER_GROUP, consumerName),
                StreamReadOptions.empty().count(10).block(Duration.ZERO),
                StreamOffset.create(RedisBackgroundJobService.STREAM_KEY, ReadOffset.lastConsumed()));

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            BackgroundJobEnvelope envelope = toEnvelope(record);
            processOne(envelope);
            redisTemplate.opsForStream().acknowledge(
                    RedisBackgroundJobService.STREAM_KEY,
                    CONSUMER_GROUP,
                    record.getId());
        }
    }

    private void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(
                    RedisBackgroundJobService.STREAM_KEY,
                    ReadOffset.from("0-0"),
                    CONSUMER_GROUP);
        } catch (Exception ignored) {
            // Group already exists or stream is not ready yet.
        }
    }

    private BackgroundJobEnvelope toEnvelope(MapRecord<String, Object, Object> record) {
        Map<?, ?> value = record.getValue();
        String correlationId = stringValue(value, "correlationId");
        return new BackgroundJobEnvelope(
                stringValue(value, "jobId"),
                BackgroundJobType.valueOf(stringValue(value, "type")),
                stringValue(value, "payload"),
                Integer.parseInt(stringValue(value, "attempt", "0")),
                Instant.parse(stringValue(value, "createdAt")),
                Instant.parse(stringValue(value, "runAfter")),
                correlationId == null || correlationId.isBlank() ? null : correlationId);
    }

    private String stringValue(Map<?, ?> value, String key) {
        return stringValue(value, key, null);
    }

    private String stringValue(Map<?, ?> value, String key, String defaultValue) {
        Object raw = value.get(key);
        if (raw == null) {
            return defaultValue;
        }
        return String.valueOf(raw);
    }
}
