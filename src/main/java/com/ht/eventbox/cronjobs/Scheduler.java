package com.ht.eventbox.cronjobs;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Scheduler.class);

    //Run at 00:05 every day
    @Scheduled(cron = "0 5 0 * * ?")
    public void log() {
        logger.info("Scheduler is running at 00:05 every day");
    }
}