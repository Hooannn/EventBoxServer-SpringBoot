package com.ht.eventbox.modules.jobs;

import java.time.Instant;

public record BackgroundJobEnvelope(
        String jobId,
        BackgroundJobType type,
        String payload,
        int attempt,
        Instant createdAt,
        Instant runAfter,
        String correlationId) {

    public BackgroundJobEnvelope withAttempt(int nextAttempt, Instant nextRunAfter) {
        return new BackgroundJobEnvelope(
                jobId,
                type,
                payload,
                nextAttempt,
                createdAt,
                nextRunAfter,
                correlationId);
    }
}
