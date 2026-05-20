package com.ht.eventbox.modules.backgroundjobs;

import com.corundumstudio.socketio.SocketIOServer;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.modules.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocketJobService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SocketJobService.class);

    private final JobScheduler jobScheduler;
    private final SocketIOServer socketIOServer;
    private final OrderRepository orderRepository;

    public void enqueueStockUpdated(long eventId) {
        enqueueAfterCommit(() -> broadcastStockUpdated(eventId));
    }

    public void enqueueStockUpdated() {
        enqueueAfterCommit(() -> broadcastStockUpdated());
    }

    public void enqueueOrderApproved(long orderId) {
        enqueueAfterCommit(() -> broadcastOrderApproved(orderId));
    }

    public void enqueueOrderFulfilled(long orderId) {
        enqueueAfterCommit(() -> broadcastOrderFulfilled(orderId));
    }

    public void enqueueOrderRefunded(long orderId) {
        enqueueAfterCommit(() -> broadcastOrderRefunded(orderId));
    }

    public void enqueueTicketTracesUpdated(long ticketItemId, long eventId) {
        enqueueAfterCommit(() -> broadcastTicketTracesUpdated(ticketItemId, eventId));
    }

    public void broadcastStockUpdated(long eventId) {
        socketIOServer.getNamespace("/event")
                .getRoomOperations(String.valueOf(eventId))
                .sendEvent("stock_updated", Map.of());
    }

    public void broadcastStockUpdated() {
        socketIOServer.getNamespace("/event")
                .getBroadcastOperations()
                .sendEvent("stock_updated", Map.of());
    }

    public void broadcastOrderApproved(long orderId) {
        Order order = loadOrder(orderId);
        socketIOServer.getNamespace("/order")
                .getRoomOperations(order.getId().toString())
                .sendEvent("order_approved", Map.of(
                        "order_id", order.getId(),
                        "status", order.getStatus(),
                        "place_total", order.getPlaceTotal()));
    }

    public void broadcastOrderFulfilled(long orderId) {
        Order order = loadOrder(orderId);
        socketIOServer.getNamespace("/order")
                .getRoomOperations(order.getId().toString())
                .sendEvent("order_fulfilled", Map.of(
                        "order_id", order.getId(),
                        "status", order.getStatus(),
                        "place_total", order.getPlaceTotal()));
    }

    public void broadcastOrderRefunded(long orderId) {
        Order order = loadOrder(orderId);
        socketIOServer.getNamespace("/order")
                .getRoomOperations(order.getId().toString())
                .sendEvent("order_refunded", Map.of(
                        "order_id", order.getId(),
                        "status", order.getStatus(),
                        "place_total", order.getPlaceTotal()));
    }

    public void broadcastTicketTracesUpdated(long ticketItemId, long eventId) {
        socketIOServer.getNamespace("/ticket")
                .getRoomOperations(String.valueOf(ticketItemId))
                .sendEvent("traces_updated", Map.of("ticket_item_id", ticketItemId));

        socketIOServer.getNamespace("/event")
                .getRoomOperations(String.valueOf(eventId))
                .sendEvent("traces_updated", Map.of("ticket_item_id", ticketItemId));
    }

    private Order loadOrder(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }

    private void enqueueAfterCommit(JobLambda job) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    jobScheduler.enqueue(job);
                }
            });
            return;
        }

        jobScheduler.enqueue(job);
    }
}
