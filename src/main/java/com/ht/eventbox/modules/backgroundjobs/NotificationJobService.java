package com.ht.eventbox.modules.backgroundjobs;

import com.google.firebase.messaging.Notification;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationJobService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationJobService.class);

    private final JobScheduler jobScheduler;
    private final PushNotificationService pushNotificationService;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

    public void enqueueEventPublished(long eventId) {
        enqueueAfterCommit(() -> sendEventPublished(eventId));
    }

    public void enqueueOrderFulfilled(long orderId) {
        enqueueAfterCommit(() -> sendOrderFulfilled(orderId));
    }

    public void enqueueOrderRefunded(long orderId) {
        enqueueAfterCommit(() -> sendOrderRefunded(orderId));
    }

    public void sendEventPublished(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found: " + eventId));

        String sql = "SELECT user_id FROM subscriptions WHERE organization_id = ?";
        List<Long> subscribers = jdbcTemplate.queryForList(sql, Long.class, event.getOrganization().getId());

        try {
            pushNotificationService.push(
                    subscribers,
                    Notification.builder()
                            .setBody(event.getTitle())
                            .setTitle("Sự kiện mới từ " + event.getOrganization().getName())
                            .setImage(event.getAssets().stream()
                                    .filter(asset -> asset.getUsage() == AssetUsage.EVENT_LOGO)
                                    .findFirst()
                                    .map(Asset::getSecureUrl)
                                    .orElse(null))
                            .build(),
                    new HashMap<>(
                            Map.of(
                                    "type", "event",
                                    "event_id", String.valueOf(event.getId())
                            )
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send event published notification for event " + eventId, e);
        }
    }

    public void sendOrderFulfilled(long orderId) {
        Order order = loadOrder(orderId);

        try {
            pushNotificationService.push(
                    order.getUser().getId(),
                    Notification.builder()
                            .setBody("Cảm ơn bạn đã đặt hàng tại EventBox. Đơn hàng của bạn đã được thanh toán thành công.")
                            .setTitle("Đơn hàng #" + order.getId() + " đã được thanh toán thành công")
                            .build(),
                    new HashMap<>(
                            Map.of(
                                    "type", "order",
                                    "order_id", String.valueOf(order.getId()))));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send order fulfilled notification for order " + orderId, e);
        }
    }

    public void sendOrderRefunded(long orderId) {
        Order order = loadOrder(orderId);

        try {
            pushNotificationService.push(
                    order.getUser().getId(),
                    Notification.builder()
                            .setBody("Có lỗi xảy ra trong quá trình xử lý đơn hàng. Số tiền của bạn đã được hoàn lại. Vui lòng kiểm tra email để biết thêm chi tiết.")
                            .setTitle("Đơn hàng #" + order.getId() + " đã được hoàn tiền thành công")
                            .build(),
                    new HashMap<>(
                            Map.of(
                                    "type", "order",
                                    "order_id", String.valueOf(order.getId()))));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send order refunded notification for order " + orderId, e);
        }
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
