package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketItemRepository extends JpaRepository<TicketItem, Long> {
    long countAllByTicketIdAndOrderStatusIn(Long ticketId, Collection<OrderStatus> statuses);

    List<TicketItem> findAllByOrderUserIdAndOrderStatusIs(Long userId, OrderStatus status);

    List<TicketItem> findAllByOrderUserIdAndOrderStatusIsOrderByIdAsc(Long userId, OrderStatus status);

    <T> List<T> findAllByTicketEventShowId(Long showId, Class<T> clazz);

    <T> List<T> findAllByOrderUserIdAndOrderStatusIsOrderByIdAsc(Long userId, OrderStatus status, Class<T> clazz);

    <T> Optional<T> findByIdAndOrderUserId(Long id, Long userId, Class<T> clazz);

    <T> Optional<T> findById(Long id, Class<T> clazz);

    <T> Optional<T> findByIdAndOrderUserIdAndOrderStatusIs(Long ticketItemId, Long userId, OrderStatus orderStatus, Class<T> clazz);

    long countAllByTicketIdAndOrderStatusInAndOrderExpiredAtIsAfter(Long id, List<OrderStatus> statuses, LocalDateTime now);

    <T> Optional<T> findByIdAndOrderStatusIs(long ticketItemId, OrderStatus orderStatus, Class<T> clazz);

    <T> Optional<T> findByIdAndOrderStatusIsAndTicketEventShowId(Long id, OrderStatus orderStatus, Long eventShowId, Class<T> clazz);
}
