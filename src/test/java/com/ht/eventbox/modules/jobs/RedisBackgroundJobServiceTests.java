package com.ht.eventbox.modules.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBackgroundJobServiceTests {
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisBackgroundJobService jobService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        jobService = new RedisBackgroundJobService(redisTemplate, new ObjectMapper());
    }

    @Test
    void enqueueSendMail_shouldWriteMailJobToStreamAndMetadata() {
        var envelope = jobService.enqueueSendMail(MailKind.REGISTRATION, Map.of(
                "recipient", "user@example.com",
                "name", "Test User",
                "otp", "123456"));

        assertThat(envelope.type()).isEqualTo(BackgroundJobType.SEND_MAIL);
        assertThat(envelope.attempt()).isZero();
        assertThat(envelope.payload()).contains("\"mailKind\":\"REGISTRATION\"");
        verify(streamOperations).add(any());
        verify(valueOperations).set(eq("jobs:meta:" + envelope.jobId()), any());
    }

    @Test
    void enqueueSendPushNotification_shouldWritePushJobToStreamAndMetadata() {
        var envelope = jobService.enqueueSendPushNotification(Map.of(
                "userIds", "1,2",
                "title", "Hello",
                "body", "World"));

        assertThat(envelope.type()).isEqualTo(BackgroundJobType.SEND_PUSH_NOTIFICATION);
        assertThat(envelope.attempt()).isZero();
        assertThat(envelope.payload()).contains("\"title\":\"Hello\"");
        verify(streamOperations).add(any());
        verify(valueOperations).set(eq("jobs:meta:" + envelope.jobId()), any());
    }
}
