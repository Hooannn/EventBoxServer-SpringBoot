package com.ht.eventbox.modules.cronjobs;

import com.ht.eventbox.modules.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Scheduler.class);
    private final OrderService orderService;

    //Run at 00:05 every day
    @Scheduled(cron = "0 5 0 * * ?")
    public void log() {
        logger.info("Scheduler is running at 00:05 every day");
    }

    //Run every 5 minutes
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredReservations() {
        logger.info("Cleaning up expired reservations");
        var count = orderService.cleanupExpiredReservations();
        logger.info("Deleted {} expired reservations", count);
    }
}