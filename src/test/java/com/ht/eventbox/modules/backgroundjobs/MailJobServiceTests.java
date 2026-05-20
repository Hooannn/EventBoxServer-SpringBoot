package com.ht.eventbox.modules.backgroundjobs;

import com.ht.eventbox.modules.mail.MailService;
import jakarta.mail.MessagingException;
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
class MailJobServiceTests {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private MailService mailService;

    @InjectMocks
    private MailJobService mailJobService;

    @Test
    void enqueueRegistrationEmailSchedulesAJob() {
        mailJobService.enqueueRegistrationEmail("user@example.com", "User Example", "123456");

        verify(jobScheduler).enqueue(any(JobLambda.class));
    }

    @Test
    void sendRegistrationEmailDelegatesToMailService() throws MessagingException {
        mailJobService.sendRegistrationEmail("user@example.com", "User Example", "123456");

        verify(mailService).sendRegistrationEmail("user@example.com", "User Example", "123456");
    }
}
