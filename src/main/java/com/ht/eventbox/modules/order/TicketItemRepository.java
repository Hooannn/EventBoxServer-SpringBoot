package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface TicketItemRepository extends JpaRepository<TicketItem, Long> {
    long countAllByTicketIdAndOrderStatusIn(Long ticketId, Collection<OrderStatus> statuses);
}
