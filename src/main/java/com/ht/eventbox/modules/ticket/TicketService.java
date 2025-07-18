package com.ht.eventbox.modules.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.order.TicketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TicketService {
    public interface EventView {
        Long getId();
        String getTitle();
        String getDescription();
        String getAddress();

        @JsonProperty("place_name")
        String getPlaceName();

        Set<Asset> getAssets();
    }

    public interface EventShowView {
        Long getId();

        EventView getEvent();

        @JsonProperty("start_time")
        java.time.LocalDateTime getStartTime();

        @JsonProperty("end_time")
        java.time.LocalDateTime getEndTime();
    }

    public interface TicketView {
        Long getId();

        String getName();

        String getDescription();

        @JsonProperty("event_show")
        EventShowView getEventShow();
    }

    public interface OrderView {
        Long getId();

        OrderStatus getStatus();

        List<Payment> getPayments();

        @JsonProperty("place_total")
        Double getPlaceTotal();

        @JsonProperty("created_at")
        java.time.LocalDateTime getCreatedAt();

        @JsonProperty("updated_at")
        java.time.LocalDateTime getUpdatedAt();
    }

    public interface TicketItemDetails {
        Long getId();

        @JsonProperty("place_total")
        Double getPlaceTotal();

        TicketView getTicket();

        OrderView getOrder();

        List<TicketItemTrace> getTraces();

        @JsonProperty("created_at")
        java.time.LocalDateTime getCreatedAt();

        @JsonProperty("updated_at")
        java.time.LocalDateTime getUpdatedAt();
    }

    private final TicketItemRepository ticketItemRepository;
    private final JwtService jwtService;

    public List<TicketItemDetails> getTicketItemsByUserIdAndOrderStatusIs(Long userId, OrderStatus status) {
        return ticketItemRepository.findAllByOrderUserIdAndOrderStatusIsOrderByIdAsc(
                userId,
                status,
                TicketItemDetails.class
        );
    }

    public String getTicketItemQrCode(Long userId, Long ticketItemId) {
        var ticketItem = ticketItemRepository.findByIdAndOrderUserIdAndOrderStatusIs(ticketItemId, userId, OrderStatus.FULFILLED, TicketItemDetails.class)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.TICKET_ITEM_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        return jwtService.generateQrCode(ticketItem.getId());
    }
}
