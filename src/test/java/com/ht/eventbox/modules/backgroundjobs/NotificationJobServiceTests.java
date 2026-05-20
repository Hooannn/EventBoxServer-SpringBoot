package com.ht.eventbox.modules.backgroundjobs;

import com.ht.eventbox.modules.messaging.PushNotificationService;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationJobServiceTests {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private NotificationJobService notificationJobService;

    @Test
    void enqueueEventPublishedSchedulesAJob() {
        notificationJobService.enqueueEventPublished(99L);

        verify(jobScheduler).enqueue(any(JobLambda.class));
    }
}
