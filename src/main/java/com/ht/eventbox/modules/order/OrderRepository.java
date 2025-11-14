package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.Order;
import com.ht.eventbox.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByItemsTicketEventShowIdAndStatusIsAndFulfilledAtBetween(
            Long eventShowId, OrderStatus status, LocalDateTime start, LocalDateTime end);

    List<Order> findAllByItemsTicketEventShowIdAndStatusIs(Long eventShowId, OrderStatus status);

    List<Order> findAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(Long showId, OrderStatus orderStatus);

    long deleteAllByStatusInAndExpiredAtBefore(List<OrderStatus> statuses, LocalDateTime now);

    long deleteAllByUserIdAndStatusIs(Long userId, OrderStatus orderStatus);

    Optional<Order> findByIdAndStatusIs(Long id, OrderStatus status);

    Optional<Order> findByIdAndStatusIn(Long id, List<OrderStatus> statuses);

    Optional<Order> findByIdAndUserIdAndStatusIn(Long orderId, Long userId, List<OrderStatus> statuses);

    Optional<Order> findByIdAndUserIdAndStatusInAndExpiredAtAfter(Long id, Long userId, Collection<OrderStatus> statuses, LocalDateTime expiredAt);

    boolean existsByVoucherId(Long voucherId);

    long countByVoucherIdAndStatusIs(Long voucherId, OrderStatus status);

    long countByUserIdAndVoucherIdAndStatusIn(Long userId, Long voucherId, List<OrderStatus> statuses);

    long countByUserIdAndVoucherId(Long userId, Long voucherId);

    long countByVoucherId(Long id);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    List<Order> findAllByItemsTicketEventShowEventIdAndStatusIs(Long eventId, OrderStatus orderStatus);
}
