package com.ht.eventbox.modules.order;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.order.dtos.CreatePaymentDto;
import com.ht.eventbox.modules.order.dtos.CreateReservationDto;
import com.ht.eventbox.modules.ticket.TicketRepository;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final CurrencyConverterService currencyConverterService;
    private final PayPalService payPalService;
    private final TicketItemRepository ticketItemRepository;

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public long cleanupExpiredReservations() {
        return orderRepository.deleteAllByStatusInAndExpiredAtBefore(
                List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING),
                java.time.LocalDateTime.now()
        );
    }

    @Transactional
    public Order createReservation(Long userId, CreateReservationDto createReservationDto) {
        orderRepository.deleteAllByUserIdAndStatusIs(userId, OrderStatus.WAITING_FOR_PAYMENT);

        List<Long> ticketIds = createReservationDto.getTickets().stream()
                .map(CreateReservationDto.ReserveTicketDto::getTicketId)
                .distinct()
                .sorted()
                .toList();

        List<Ticket> tickets = ticketRepository.findAllByIdWithLocked(ticketIds);

        Order order = Order.builder()
                .user(User.builder().id(userId).build())
                .items(new ArrayList<>())
                .build();

        createReservationDto.getTickets()
                .forEach(ticketDto -> {
                    Ticket ticket = tickets.stream()
                            .filter(t -> t.getId().equals(ticketDto.getTicketId()))
                            .findFirst()
                            .orElseThrow(() -> new HttpException(
                                    Constant.ErrorCode.TICKET_NOT_FOUND,
                                    HttpStatus.NOT_FOUND
                            ));

                    long reservationCount = ticketItemRepository
                            .countAllByTicketIdAndOrderStatusIn(
                                    ticket.getId(),
                                    List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)
                            );

                    if (reservationCount + ticketDto.getQuantity() > ticket.getStock()) {
                        throw new HttpException(
                                Constant.ErrorCode.TICKET_OUT_OF_STOCK,
                                HttpStatus.BAD_REQUEST
                        );
                    }

                    List<TicketItem> ticketItems = IntStream.range(0, ticketDto.getQuantity())
                            .mapToObj(i -> TicketItem.builder()
                                    .ticket(ticket)
                                    .order(order)
                                    .placeTotal(ticket.getPrice())
                                    .build())
                            .toList();


                    order.getItems().addAll(ticketItems);
                });

        order.setStatus(OrderStatus.WAITING_FOR_PAYMENT);
        order.setPlaceTotal(
                order.getItems().stream()
                        .mapToDouble(TicketItem::getPlaceTotal)
                        .sum()
        );
        order.setExpiredAt(
                LocalDateTime.now().plusSeconds(Constant.RedisKey.RESERVATION_EXPIRES)
        );

        return orderRepository.save(order);
    }

    @Transactional
    public boolean cancelReservation(Long userId) {
        var count = orderRepository.deleteAllByUserIdAndStatusIs(userId, OrderStatus.WAITING_FOR_PAYMENT);
        return count > 0;
    }

    public com.paypal.sdk.models.Order createPayment(Long userId, CreatePaymentDto createPaymentDto) {
        Order order = orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                        createPaymentDto.getOrderId(),
                        userId,
                        List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING),
                        LocalDateTime.now()
                ).orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.ORDER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        ApiResponse<com.paypal.sdk.models.Order> response = null;
        try {
            var usdAmount = currencyConverterService.convertVndToUsd(order.getPlaceTotal());
            response = payPalService.createOrder(
                    order.getId().toString(),
                    usdAmount,
                    "USD",
                    "Thanh toán đơn hàng #" + order.getId(),
                    createPaymentDto.getCancelUrl(),
                    createPaymentDto.getReturnUrl()
            );
        }  catch (IOException | ApiException e) {
            throw new HttpException("Lỗi khi tạo đơn hàng PayPal: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (response.getStatusCode() != 201 && response.getStatusCode() != 200) {
            throw new HttpException("Không thể tạo đơn hàng PayPal", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        order.setExpiredAt(
                LocalDateTime.now().plusSeconds(Constant.RedisKey.EXTENDED_RESERVATION_EXPIRES)
        );
        order.setStatus(OrderStatus.PENDING);

        orderRepository.save(order);
        return response.getResult();
    }

    public Order findById(long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public Order fulfill(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));

        order.setStatus(OrderStatus.FULFILLED);

        List<Ticket> tickets = ticketRepository.findAllByIdWithLocked(
                order.getItems().stream()
                        .map(TicketItem::getTicket)
                        .map(Ticket::getId)
                        .distinct()
                        .toList()
        );

        tickets.forEach(ticket -> {
            long reservedCount = order.getItems().stream()
                    .filter(item -> item.getTicket().getId().equals(ticket.getId()))
                    .count();

            ticket.setStock((int) ((long) ticket.getStock() - reservedCount));
        });

        ticketRepository.saveAll(tickets);

        return orderRepository.save(order);
    }
}
