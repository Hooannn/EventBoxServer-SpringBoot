package com.ht.eventbox.modules.backgroundjobs;

import com.corundumstudio.socketio.SocketIOServer;
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
class SocketJobServiceTests {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private SocketIOServer socketIOServer;

    @InjectMocks
    private SocketJobService socketJobService;

    @Test
    void enqueueStockUpdatedSchedulesAJob() {
        socketJobService.enqueueStockUpdated(42L);

        verify(jobScheduler).enqueue(any(JobLambda.class));
    }
}
