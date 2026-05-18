package com.ht.eventbox.modules.order;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Payment;
import com.ht.eventbox.entities.PaymentSession;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.entities.UserOrganization;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.jobs.MailKind;
import com.ht.eventbox.modules.jobs.RedisBackgroundJobService;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.ticket.TicketRepository;
import com.ht.eventbox.modules.order.dtos.CreateReservationDto;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.Money;
import com.paypal.sdk.models.PayeeBase;
import com.paypal.sdk.models.OrdersCapture;
import com.paypal.sdk.models.PaymentCollection;
import com.paypal.sdk.models.PurchaseUnit;
import com.paypal.sdk.models.Refund;
import com.paypal.sdk.models.SellerPayableBreakdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentSessionRepository paymentSessionRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CurrencyConverterServiceV2 currencyConverterService;

    @Mock
    private PayPalService payPalService;

    @Mock
    private TicketItemRepository ticketItemRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RedisBackgroundJobService redisBackgroundJobService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private SocketIOServer socketIOServer;

    @Spy
    @InjectMocks
    private OrderService orderService;

    @Mock
    private SocketIONamespace eventNamespace;

    @Mock
    private BroadcastOperations eventBroadcastOperations;

    @Mock
    private SocketIONamespace orderNamespace;

    @Mock
    private BroadcastOperations orderBroadcastOperations;

    @BeforeEach
    void setUp() {
        lenient().when(socketIOServer.getNamespace("/event")).thenReturn(eventNamespace);
        lenient().when(eventNamespace.getBroadcastOperations()).thenReturn(eventBroadcastOperations);
        lenient().when(eventNamespace.getRoomOperations(anyString())).thenReturn(eventBroadcastOperations);
        lenient().when(socketIOServer.getNamespace("/order")).thenReturn(orderNamespace);
        lenient().when(orderNamespace.getRoomOperations(anyString())).thenReturn(orderBroadcastOperations);
    }

    @Test
    void createReservation_shouldPersistOrderAndReserveTickets() {
        var now = LocalDateTime.now();
        var ticket = sampleTicket(now.minusHours(1), now.plusHours(2), now.minusHours(2), now.plusHours(1));
        var reserveTicket = new CreateReservationDto.ReserveTicketDto();
        reserveTicket.setTicketId(100L);
        reserveTicket.setQuantity(2);
        var dto = CreateReservationDto.builder()
                .tickets(List.of(reserveTicket))
                .build();

        when(orderRepository.deleteAllByUserIdAndStatusIs(42L, OrderStatus.WAITING_FOR_PAYMENT)).thenReturn(1L);
        when(ticketRepository.findAllByIdWithLocked(List.of(100L))).thenReturn(List.of(ticket));
        when(ticketItemRepository.countAllByTicketIdAndOrderStatusInAndOrderExpiredAtIsAfter(
                eq(100L),
                anyList(),
                any())).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(500L);
            return order;
        });

        var savedOrder = orderService.createReservation(42L, dto);

        assertThat(savedOrder.getId()).isEqualTo(500L);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_PAYMENT);
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getPlaceTotal()).isEqualTo(100000.0);
        assertThat(savedOrder.getExpiredAt()).isAfter(LocalDateTime.now().minusSeconds(5));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getUser().getId()).isEqualTo(42L);
        verify(orderRepository).deleteAllByUserIdAndStatusIs(42L, OrderStatus.WAITING_FOR_PAYMENT);
        verify(ticketRepository).findAllByIdWithLocked(List.of(100L));
        verify(ticketItemRepository).countAllByTicketIdAndOrderStatusInAndOrderExpiredAtIsAfter(
                eq(100L),
                anyList(),
                any());
    }

    @Test
    void createReservation_shouldRejectWhenSaleHasNotStarted() {
        var now = LocalDateTime.now();
        var ticket = sampleTicket(now.plusHours(1), now.plusHours(3), now.minusHours(2), now.plusHours(2));
        var reserveTicket = new CreateReservationDto.ReserveTicketDto();
        reserveTicket.setTicketId(100L);
        reserveTicket.setQuantity(1);
        var dto = CreateReservationDto.builder()
                .tickets(List.of(reserveTicket))
                .build();

        when(orderRepository.deleteAllByUserIdAndStatusIs(42L, OrderStatus.WAITING_FOR_PAYMENT)).thenReturn(1L);
        when(ticketRepository.findAllByIdWithLocked(List.of(100L))).thenReturn(List.of(ticket));

        assertThatThrownBy(() -> orderService.createReservation(42L, dto))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.TICKET_SALE_NOT_STARTED);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });

        verify(orderRepository, never()).save(any(Order.class));
        verify(ticketItemRepository, never()).countAllByTicketIdAndOrderStatusInAndOrderExpiredAtIsAfter(
                anyLong(), anyList(), any());
    }

    @Test
    void cancelReservation_shouldRemoveWaitingOrdersAndReturnTrue() {
        when(orderRepository.deleteAllByUserIdAndStatusIs(42L, OrderStatus.WAITING_FOR_PAYMENT)).thenReturn(1L);

        boolean result = orderService.cancelReservation(42L);

        assertThat(result).isTrue();
        verify(orderRepository).deleteAllByUserIdAndStatusIs(42L, OrderStatus.WAITING_FOR_PAYMENT);
    }

    @Test
    void cancelReservation_shouldDeleteSpecificOrderAndReturnTrue() {
        when(orderRepository.deleteByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(500L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any())).thenReturn(1L);

        boolean result = orderService.cancelReservation(42L, 500L);

        assertThat(result).isTrue();
        verify(orderRepository).deleteByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(500L),
                eq(42L),
                eq(List.of(OrderStatus.WAITING_FOR_PAYMENT, OrderStatus.PENDING)),
                any());
    }

    @Test
    void getByShowId_shouldRejectUsersWithoutManagerOrOwnerRole() {
        var event = Event.builder()
                .id(7L)
                .organization(Organization.builder()
                        .id(9L)
                        .userOrganizations(List.of(
                                UserOrganization.builder()
                                        .user(User.builder().id(99L).build())
                                        .role(OrganizationRole.STAFF)
                                        .build()))
                        .build())
                .status(EventStatus.PUBLISHED)
                .build();

        when(eventRepository.findByShowsId(55L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> orderService.getByShowId(42L, 55L, LocalDateTime.now().minusDays(1), LocalDateTime.now()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.NOT_ALLOWED_OPERATION);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });

        verifyNoInteractions(orderRepository);
    }

    @Test
    void createPayment_shouldReuseExistingPaypalOrderWhenSessionExists() throws Exception {
        var order = sampleOrder();
        var paymentSession = PaymentSession.builder()
                .order(order)
                .provider("paypal")
                .paypalOrderId("paypal-order-1")
                .build();
        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-1");

        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(500L),
                eq(42L),
                anyList(),
                any())).thenReturn(Optional.of(order));
        when(paymentSessionRepository.findByOrderIdAndProvider(500L, "paypal")).thenReturn(Optional.of(paymentSession));
        when(payPalService.getOrderById("paypal-order-1")).thenReturn(new ApiResponse<>(200, null, paypalOrder));

        var result = orderService.createPayment(42L, com.ht.eventbox.modules.order.dtos.CreatePaymentDto.builder()
                .orderId(500L)
                .cancelUrl("https://cancel")
                .returnUrl("https://return")
                .build());

        assertThat(result).isEqualTo(paypalOrder);
        verify(payPalService).getOrderById("paypal-order-1");
        verifyNoInteractions(currencyConverterService);
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentSessionRepository, never()).save(any(PaymentSession.class));
    }

    @Test
    void createPayment_shouldCreateNewPaypalOrderAndSession() throws Exception {
        var order = sampleOrder();
        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-2");
        var apiResponse = new ApiResponse<>(201, null, paypalOrder);

        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(500L),
                eq(42L),
                anyList(),
                any())).thenReturn(Optional.of(order));
        when(paymentSessionRepository.findByOrderIdAndProvider(500L, "paypal")).thenReturn(Optional.empty());
        when(currencyConverterService.convertVndToUsd(100000.0)).thenReturn(4.0);
        when(payPalService.createOrder(eq("500"), eq(4.0), eq("USD"), anyString(), eq("https://cancel"), eq("https://return")))
                .thenReturn(apiResponse);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentSessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = orderService.createPayment(42L, com.ht.eventbox.modules.order.dtos.CreatePaymentDto.builder()
                .orderId(500L)
                .cancelUrl("https://cancel")
                .returnUrl("https://return")
                .build());

        assertThat(result).isEqualTo(paypalOrder);
        verify(currencyConverterService).convertVndToUsd(100000.0);
        verify(orderRepository).save(order);
        verify(paymentSessionRepository).save(org.mockito.ArgumentMatchers.argThat((PaymentSession session) ->
                session != null
                        && "paypal".equals(session.getProvider())
                        && "paypal-order-2".equals(session.getPaypalOrderId())));
    }

    @Test
    void createPayment_shouldRejectMissingOrder() {
        when(orderRepository.findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(500L),
                eq(42L),
                anyList(),
                any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createPayment(42L, com.ht.eventbox.modules.order.dtos.CreatePaymentDto.builder()
                .orderId(500L)
                .cancelUrl("https://cancel")
                .returnUrl("https://return")
                .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORDER_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });

        verify(orderRepository).findByIdAndUserIdAndStatusInAndExpiredAtAfter(
                eq(500L),
                eq(42L),
                anyList(),
                any());
        verifyNoInteractions(paymentSessionRepository, payPalService, currencyConverterService);
    }

    @Test
    void processPayment_shouldApproveAndFulfillActiveOrder() throws Exception {
        var order = sampleOrder();
        order.setStatus(OrderStatus.PENDING);
        order.setExpiredAt(LocalDateTime.now().plusMinutes(15));

        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-3");
        paypalOrder.setStatus(com.paypal.sdk.models.OrderStatus.COMPLETED);
        paypalOrder.setPurchaseUnits(List.of(purchaseUnit("42", "capture-1")));
        var capture = new OrdersCapture();
        capture.setId("capture-1");
        capture.setFinalCapture(true);
        capture.setAmount(new Money("USD", "100.00"));
        var paymentCollection = new PaymentCollection();
        paymentCollection.setCaptures(List.of(capture));
        paypalOrder.getPurchaseUnits().get(0).setPayments(paymentCollection);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(payPalService.captureOrder("paypal-order-3")).thenReturn(new ApiResponse<>(200, null, paypalOrder));
        doNothing().when(orderService).onOrderApproved(any(Order.class));
        doNothing().when(orderService).onOrderFulfilled(any(Order.class));
        doReturn(order).when(orderService).fulfill(any(Order.class));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.processPayment(500L, "paypal-order-3");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        verify(paymentService).createFromOrderAndPaypalOrder(order, paypalOrder);
        verify(paymentService).fulfillPaymentFromPaypalOrder(capture, paypalOrder);
        verify(orderRepository).save(order);
        verify(orderService).fulfill(order);
    }

    @Test
    void processPayment_shouldRefundExpiredOrder() throws Exception {
        var order = sampleOrder();
        order.setStatus(OrderStatus.PENDING);
        order.setExpiredAt(LocalDateTime.now().minusMinutes(5));

        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-4");
        paypalOrder.setStatus(com.paypal.sdk.models.OrderStatus.COMPLETED);
        paypalOrder.setPurchaseUnits(List.of(purchaseUnit("42", "capture-2")));
        var capture = new OrdersCapture();
        capture.setId("capture-2");
        capture.setFinalCapture(true);
        capture.setAmount(new Money("USD", "100.00"));
        var paymentCollection = new PaymentCollection();
        paymentCollection.setCaptures(List.of(capture));
        paypalOrder.getPurchaseUnits().get(0).setPayments(paymentCollection);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(payPalService.captureOrder("paypal-order-4")).thenReturn(new ApiResponse<>(200, null, paypalOrder));
        doNothing().when(orderService).onOrderApproved(any(Order.class));
        doNothing().when(orderService).refund(any(Order.class), eq("capture-2"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.processPayment(500L, "paypal-order-4");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderService).refund(order, "capture-2");
    }

    @Test
    void refund_shouldSaveMissedRefundWhenPaypalDoesNotReturnSuccess() throws Exception {
        var payment = Payment.builder()
                .id(1L)
                .paypalCaptureId("capture-1")
                .order(sampleOrder())
                .build();
        var order = sampleOrder();
        order.setId(500L);

        when(paymentRepository.findByPaypalCaptureId("capture-1")).thenReturn(Optional.of(payment));
        when(payPalService.refundCapture(eq("capture-1"), anyString()))
                .thenReturn(new ApiResponse<>(500, null, new Refund()));

        orderService.refund(order, "capture-1");

        verify(refundRepository).save(org.mockito.ArgumentMatchers.argThat((com.ht.eventbox.entities.Refund refund) ->
                refund != null
                        && "MISSED".equals(refund.getStatus())));
    }

    @Test
    void refund_shouldPersistFullRefundDetailsAndTriggerNotifications() throws Exception {
        var order = sampleOrder();
        order.setStatus(OrderStatus.FULFILLED);
        var payment = Payment.builder()
                .id(1L)
                .paypalCaptureId("capture-2")
                .order(order)
                .build();

        when(paymentRepository.findByPaypalCaptureId("capture-2")).thenReturn(Optional.of(payment));
        when(payPalService.refundCapture(eq("capture-2"), anyString()))
                .thenReturn(new ApiResponse<>(200, null, sampleRefund()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> pushPayloadCaptor = ArgumentCaptor.forClass(Map.class);

        orderService.refund(order, "capture-2");

        verify(refundRepository).save(org.mockito.ArgumentMatchers.argThat((com.ht.eventbox.entities.Refund refund) ->
                refund != null
                        && "COMPLETED".equals(refund.getStatus())
                        && "refund-1".equals(refund.getPaypalRefundId())
                        && "capture-2".equals(refund.getPayment().getPaypalCaptureId())
                        && "REF-123".equals(refund.getPayerMerchantId())
                        && "payer@example.com".equals(refund.getPayerEmail())));
        verify(redisBackgroundJobService).enqueueSendMail(eq(MailKind.ORDER_REFUNDED), payloadCaptor.capture());
        verify(redisBackgroundJobService).enqueueSendPushNotification(pushPayloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("recipient", "user@example.com")
                .containsEntry("name", "Test User")
                .containsEntry("orderId", "500");
        assertThat(payloadCaptor.getValue().get("amount")).isNotBlank();
        assertThat(payloadCaptor.getValue().get("timestamp")).isNotBlank();
        assertThat(pushPayloadCaptor.getValue())
                .containsEntry("userIds", "42")
                .containsEntry("title", "Đơn hàng #500 đã được hoàn tiền thành công")
                .containsEntry("body", "Có lỗi xảy ra trong quá trình xử lý đơn hàng. Số tiền của bạn đã được hoàn lại. Vui lòng kiểm tra email để biết thêm chi tiết.")
                .containsEntry("type", "order")
                .containsEntry("order_id", "500");
    }

    @Test
    void onOrderFulfilled_shouldEnqueuePaidMailJob() {
        var order = sampleOrder();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> pushPayloadCaptor = ArgumentCaptor.forClass(Map.class);

        orderService.onOrderFulfilled(order);

        verify(redisBackgroundJobService).enqueueSendMail(eq(MailKind.ORDER_PAID), payloadCaptor.capture());
        verify(redisBackgroundJobService).enqueueSendPushNotification(pushPayloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("recipient", "user@example.com")
                .containsEntry("name", "Test User")
                .containsEntry("orderId", "500");
        assertThat(payloadCaptor.getValue().get("amount")).isNotBlank();
        assertThat(payloadCaptor.getValue().get("timestamp")).isNotBlank();
        assertThat(pushPayloadCaptor.getValue())
                .containsEntry("userIds", "42")
                .containsEntry("title", "Đơn hàng #500 đã được thanh toán thành công")
                .containsEntry("body", "Cảm ơn bạn đã đặt hàng tại EventBox. Đơn hàng của bạn đã được thanh toán thành công.")
                .containsEntry("type", "order")
                .containsEntry("order_id", "500");
    }

    @Test
    void getByShowId_shouldReturnOrdersForAuthorizedManager() {
        var event = Event.builder()
                .id(7L)
                .organization(Organization.builder()
                        .id(9L)
                        .userOrganizations(List.of(
                                UserOrganization.builder()
                                        .user(User.builder().id(42L).build())
                                        .role(OrganizationRole.MANAGER)
                                        .build()))
                        .build())
                .status(EventStatus.PUBLISHED)
                .build();
        var order = sampleOrder();

        when(eventRepository.findByShowsId(55L)).thenReturn(Optional.of(event));
        when(orderRepository.findAllByItemsTicketEventShowIdAndStatusIsAndFulfilledAtBetween(
                eq(55L), eq(OrderStatus.FULFILLED), any(), any())).thenReturn(List.of(order));

        var result = orderService.getByShowId(42L, 55L, LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(result).containsExactly(order);
        verify(orderRepository).findAllByItemsTicketEventShowIdAndStatusIsAndFulfilledAtBetween(
                eq(55L), eq(OrderStatus.FULFILLED), any(), any());
    }

    @Test
    void getByShowId_shouldRejectMissingEvent() {
        when(eventRepository.findByShowsId(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByShowId(42L, 55L, LocalDateTime.now().minusDays(1), LocalDateTime.now()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.EVENT_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    private Ticket sampleTicket(LocalDateTime saleStartTime, LocalDateTime saleEndTime,
                                LocalDateTime startTime, LocalDateTime endTime) {
        return Ticket.builder()
                .id(100L)
                .price(50000.0)
                .stock(10)
                .initialStock(10)
                .available(true)
                .eventShow(EventShow.builder()
                        .id(200L)
                        .event(Event.builder().id(7L).build())
                        .saleStartTime(saleStartTime)
                        .saleEndTime(saleEndTime)
                        .startTime(startTime)
                        .endTime(endTime)
                        .title("Sample Show")
                        .build())
                .build();
    }

    private Order sampleOrder() {
        var event = Event.builder().id(7L).build();
        var eventShow = EventShow.builder().id(200L).event(event).build();
        var ticket = Ticket.builder()
                .id(100L)
                .price(50000.0)
                .stock(10)
                .initialStock(10)
                .available(true)
                .eventShow(eventShow)
                .build();
        var ticketItem = TicketItem.builder()
                .ticket(ticket)
                .placeTotal(50000.0)
                .build();
        return Order.builder()
                .id(500L)
                .user(User.builder().id(42L).email("user@example.com").firstName("Test").lastName("User").build())
                .status(OrderStatus.WAITING_FOR_PAYMENT)
                .placeTotal(100000.0)
                .items(new ArrayList<>(List.of(ticketItem)))
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    private PurchaseUnit purchaseUnit(String customId, String captureId) {
        var purchaseUnit = new PurchaseUnit();
        purchaseUnit.setCustomId(customId);
        var paymentCollection = new PaymentCollection();
        var capture = new OrdersCapture();
        capture.setId(captureId);
        capture.setFinalCapture(true);
        capture.setAmount(new Money("USD", "100.00"));
        paymentCollection.setCaptures(List.of(capture));
        purchaseUnit.setPayments(paymentCollection);
        return purchaseUnit;
    }

    private Refund sampleRefund() {
        var refund = new Refund();
        refund.setId("refund-1");
        refund.setStatus(com.paypal.sdk.models.RefundStatus.COMPLETED);
        refund.setCustomId("42");
        refund.setCreateTime("2026-05-16T00:00:00Z");
        refund.setUpdateTime("2026-05-16T00:10:00Z");
        refund.setNoteToPayer("Refund for order #500 due to late payment");
        refund.setAcquirerReferenceNumber("ARN-1");
        refund.setAmount(new Money("USD", "100.00"));
        var payer = new PayeeBase();
        payer.setEmailAddress("payer@example.com");
        payer.setMerchantId("REF-123");
        refund.setPayer(payer);

        var breakdown = new SellerPayableBreakdown();
        breakdown.setGrossAmount(new Money("USD", "100.00"));
        breakdown.setNetAmount(new Money("USD", "95.00"));
        breakdown.setPaypalFee(new Money("USD", "5.00"));
        breakdown.setTotalRefundedAmount(new Money("USD", "100.00"));
        refund.setSellerPayableBreakdown(breakdown);
        return refund;
    }
}
