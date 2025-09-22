package com.ht.eventbox.modules.cronjobs;

import com.corundumstudio.socketio.SocketIOServer;
import com.ht.eventbox.modules.order.OrderService;
import com.ht.eventbox.modules.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Scheduler.class);
    private final OrderService orderService;
    private final TicketService ticketService;
    private final SocketIOServer socketIOServer;

    //Run at 00:01 every day
    @Scheduled(cron = "0 1 0 * * ?")
    public void remindUpcomingEvents() {
        logger.info("Reminding upcoming events");
        ticketService.remindUpcomingEvents();
    }

    //Run every 5 minutes
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredReservations() {
        logger.info("Cleaning up expired reservations");

        var count = orderService.cleanupExpiredReservations();

        logger.info("Deleted {} expired reservations", count);

        if (count > 0) {
            socketIOServer.getNamespace("/event")
                    .getBroadcastOperations()
                    .sendEvent("stock_updated", Map.of());
        }
    }
}