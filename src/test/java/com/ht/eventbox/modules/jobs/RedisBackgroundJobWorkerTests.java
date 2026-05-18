package com.ht.eventbox.modules.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBackgroundJobWorkerTests {
    @Mock
    private RedisBackgroundJobRetryScheduler retryScheduler;

    @Mock
    private RedisBackgroundJobHandler mailHandler;

    @Mock
    private RedisBackgroundJobHandler pushHandler;

    @Test
    void processOne_shouldDispatchMailJob() throws Exception {
        when(mailHandler.supports()).thenReturn(BackgroundJobType.SEND_MAIL);
        when(pushHandler.supports()).thenReturn(BackgroundJobType.SEND_PUSH_NOTIFICATION);
        var worker = new RedisBackgroundJobWorker(List.of(mailHandler, pushHandler), retryScheduler);
        var envelope = new BackgroundJobEnvelope("job-1", BackgroundJobType.SEND_MAIL, "{}", 0, Instant.now(), Instant.now(), null);

        worker.processOne(envelope);

        verify(mailHandler).handle(envelope);
    }

    @Test
    void processOne_shouldRetryTransientFailure() throws Exception {
        when(mailHandler.supports()).thenReturn(BackgroundJobType.SEND_MAIL);
        var worker = new RedisBackgroundJobWorker(List.of(mailHandler), retryScheduler);
        var envelope = new BackgroundJobEnvelope("job-1", BackgroundJobType.SEND_MAIL, "{}", 0, Instant.now(), Instant.now(), null);
        when(retryScheduler.shouldRetry(envelope)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(mailHandler).handle(envelope);

        worker.processOne(envelope);

        verify(retryScheduler).scheduleRetry(org.mockito.ArgumentMatchers.eq(envelope), org.mockito.ArgumentMatchers.any(RuntimeException.class));
    }

    @Test
    void processOne_shouldDeadLetterAfterMaxAttempts() throws Exception {
        when(mailHandler.supports()).thenReturn(BackgroundJobType.SEND_MAIL);
        var worker = new RedisBackgroundJobWorker(List.of(mailHandler), retryScheduler);
        var envelope = new BackgroundJobEnvelope("job-1", BackgroundJobType.SEND_MAIL, "{}", 2, Instant.now(), Instant.now(), null);
        when(retryScheduler.shouldRetry(envelope)).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(mailHandler).handle(envelope);

        worker.processOne(envelope);

        verify(retryScheduler).moveToDeadLetter(org.mockito.ArgumentMatchers.eq(envelope), org.mockito.ArgumentMatchers.any(RuntimeException.class));
    }
}
