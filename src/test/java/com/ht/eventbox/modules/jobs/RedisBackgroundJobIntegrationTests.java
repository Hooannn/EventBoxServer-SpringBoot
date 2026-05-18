package com.ht.eventbox.modules.jobs;

import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.event.EventShowRepository;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.support.RedisTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = RedisBackgroundJobIntegrationTests.TestApplication.class)
@ActiveProfiles("test")
class RedisBackgroundJobIntegrationTests extends RedisTestSupport {
    @Autowired
    private RedisBackgroundJobService jobService;

    @Autowired
    private RedisBackgroundJobWorker worker;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private MailService mailService;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private EventShowRepository eventShowRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @Test
    void pollOnce_shouldHandleMailJobAndAckTheStreamEntry() throws Exception {
        jobService.enqueueSendMail(MailKind.REGISTRATION, Map.of(
                "recipient", "user@example.com",
                "name", "Test User",
                "otp", "123456"));

        worker.pollOnce();

        verify(mailService).sendRegistrationEmail(eq("user@example.com"), eq("Test User"), eq("123456"));
        PendingMessagesSummary summary = redisTemplate.opsForStream().pending(
                RedisBackgroundJobService.STREAM_KEY,
                RedisBackgroundJobWorker.CONSUMER_GROUP);
        assertThat(summary.getTotalPendingMessages()).isZero();
    }

    @Test
    void pollOnce_shouldScheduleRetryWhenMailHandlerFails() throws Exception {
        doThrow(new RuntimeException("boom")).when(mailService).sendRegistrationEmail(
                eq("retry@example.com"),
                eq("Retry User"),
                eq("999999"));

        jobService.enqueueSendMail(MailKind.REGISTRATION, Map.of(
                "recipient", "retry@example.com",
                "name", "Retry User",
                "otp", "999999"));

        worker.pollOnce();

        assertThat(redisTemplate.opsForZSet().size(RedisBackgroundJobRetryScheduler.RETRY_KEY)).isEqualTo(1L);
    }

    @EnableAutoConfiguration
    @Import({
            RedisBackgroundJobService.class,
            RedisBackgroundJobWorker.class,
            RedisBackgroundJobRetryScheduler.class,
            MailJobHandler.class,
            PushNotificationJobHandler.class
    })
    static class TestApplication {
    }
}
