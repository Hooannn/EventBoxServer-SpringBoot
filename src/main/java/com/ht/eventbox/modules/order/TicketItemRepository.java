package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.ticket.TicketService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketItemRepository extends JpaRepository<TicketItem, Long> {
    long countAllByTicketIdAndOrderStatusIn(Long ticketId, Collection<OrderStatus> statuses);

    List<TicketItem> findAllByOrderUserIdAndOrderStatusIs(Long userId, OrderStatus status);

    List<TicketItem> findAllByOrderUserIdAndOrderStatusIsOrderByIdAsc(Long userId, OrderStatus status);

    <T> List<T> findAllByOrderUserIdAndOrderStatusIsOrderByIdAsc(Long userId, OrderStatus status, Class<T> clazz);

    <T> Optional<T> findByIdAndOrderUserId(Long id, Long userId, Class<T> clazz);

    <T> Optional<T> findByIdAndOrderUserIdAndOrderStatusIs(Long ticketItemId, Long userId, OrderStatus orderStatus, Class<T> clazz);
}
