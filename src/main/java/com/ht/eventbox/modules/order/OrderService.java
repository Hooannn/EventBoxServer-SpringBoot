package com.ht.eventbox.modules.order;

import com.corundumstudio.socketio.SocketIOServer;
import com.google.firebase.messaging.Notification;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.order.dtos.CreatePaymentDto;
import com.ht.eventbox.modules.order.dtos.CreateReservationDto;
import com.ht.eventbox.modules.ticket.TicketRepository;
import com.ht.eventbox.utils.Helper;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final CurrencyConverterServiceV2 currencyConverterService;
    private final PayPalService payPalService;
    private final TicketItemRepository ticketItemRepository;
    private final MailService mailService;
    private final PushNotificationService pushNotificationService;
    private final SocketIOServer socketIOServer;

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

    public void onStockUpdated(long eventId) {
        CompletableFuture.runAsync(() -> {
            socketIOServer.getNamespace("/event")
                    .getRoomOperations(String.valueOf(eventId))
                    .sendEvent("stock_updated", Map.of());
        });
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

        tickets.forEach(ticket -> {
            if (ticket.getEventShow().getSaleStartTime().isAfter(LocalDateTime.now())) {
                throw new HttpException(
                        Constant.ErrorCode.TICKET_SALE_NOT_STARTED,
                        HttpStatus.BAD_REQUEST
                );
            }

            if (ticket.getEventShow().getSaleEndTime().isBefore(LocalDateTime.now())) {
                throw new HttpException(
                        Constant.ErrorCode.TICKET_SALE_ENDED,
                        HttpStatus.BAD_REQUEST
                );
            }
        });

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
                            .countAllByTicketIdAndOrderStatusInAndOrderExpiredAtIsAfter(
                                    ticket.getId(),
                                    List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING),
                                    LocalDateTime.now()
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

        var savedOrder = orderRepository.save(order);

        if (!tickets.isEmpty()) {
            onStockUpdated(
                    tickets.get(0).getEventShow().getEvent().getId()
            );
        }

        return savedOrder;
    }

    @Transactional
    public boolean cancelReservation(Long userId) {
        var count = orderRepository.deleteAllByUserIdAndStatusIs(userId, OrderStatus.WAITING_FOR_PAYMENT);

        if (count > 0) {
            CompletableFuture.runAsync(() -> {
                socketIOServer.getNamespace("/event")
                        .getBroadcastOperations()
                        .sendEvent("stock_updated", Map.of());
            });
        }

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
        order.setFulfilledAt(LocalDateTime.now());

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

        var savedOrder = orderRepository.save(order);

        if (!tickets.isEmpty()) {
            onStockUpdated(
                    tickets.get(0).getEventShow().getEvent().getId()
            );
        }

        return savedOrder;
    }


    public void onOrderFulfilled(Order order) {
        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendOrderPaidMail(
                        order.getUser().getEmail(),
                        order.getUser().getFullName(),
                        order.getId().toString(),
                        Helper.formatCurrencyToString(order.getPlaceTotal()),
                        Helper.formatDateToString(LocalDateTime.now())
                );
            } catch (Exception e) {
                logger.error("Có lỗi xảy ra khi gửi mail: {}", e.getMessage());
            }
        });

        CompletableFuture.runAsync(() -> {
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
                                        "order_id", String.valueOf(order.getId())
                                )
                        )
                );
            } catch (Exception e) {
                logger.error("Error sending push notification for order {}: {}", order.getId(), e.getMessage());
            }
        });

        CompletableFuture.runAsync(() -> {
            socketIOServer.getNamespace("/order")
                    .getRoomOperations(order.getId().toString())
                    .sendEvent("order_fulfilled", Map.of(
                            "order_id", order.getId(),
                            "status", order.getStatus(),
                            "place_total", order.getPlaceTotal()
                    ));
        });
    }

    public List<Order> getByShowId(Long userId, Long showId, LocalDateTime from, LocalDateTime to) {
        Event event = eventRepository.findByShowsId(showId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        var members = event.getOrganization().getUserOrganizations();

        if (members.stream().noneMatch(m -> m.getUser().getId().equals(userId) && List.of(OrganizationRole.MANAGER, OrganizationRole.OWNER).contains(m.getRole()))) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        return orderRepository.findAllByItemsTicketEventShowIdAndStatusIsAndFulfilledAtBetween(
                showId,
                OrderStatus.FULFILLED,
                from,
                to
        );
    }

    public List<Order> getByShowId(Long userId, Long showId) {
        Event event = eventRepository.findByShowsId(showId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        var members = event.getOrganization().getUserOrganizations();

        if (members.stream().noneMatch(m -> m.getUser().getId().equals(userId) && List.of(OrganizationRole.MANAGER, OrganizationRole.OWNER).contains(m.getRole()))) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        return orderRepository.findAllByItemsTicketEventShowIdAndStatusIs(
                showId,
                OrderStatus.FULFILLED
        );
    }
}
