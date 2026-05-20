package com.ht.eventbox.config;

import com.ht.eventbox.support.AbstractSpringBootTest;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class JobRunrBootstrapTests extends AbstractSpringBootTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Test
    void jobSchedulerBeanIsAvailable() {
        assertThat(jobScheduler).isNotNull();
    }
}
