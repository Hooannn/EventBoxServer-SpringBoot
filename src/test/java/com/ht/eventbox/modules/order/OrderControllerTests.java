package com.ht.eventbox.modules.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Payment;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.order.dtos.CreateReservationDto;
import com.ht.eventbox.modules.order.dtos.CreatePaymentDto;
import com.ht.eventbox.modules.order.dtos.PaymentWebhookDto;
import com.paypal.sdk.http.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private PayPalService payPalService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void createReservation_shouldReturnCreatedOrder() throws Exception {
        var reserveTicket = new CreateReservationDto.ReserveTicketDto();
        reserveTicket.setTicketId(100L);
        reserveTicket.setQuantity(1);

        var responseOrder = Order.builder()
                .id(55L)
                .status(OrderStatus.WAITING_FOR_PAYMENT)
                .placeTotal(50000.0)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(orderService.createReservation(eq(42L), any(CreateReservationDto.class))).thenReturn(responseOrder);

                mockMvc.perform(post("/api/v1/orders/reservation")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateReservationDto.builder()
                                .tickets(java.util.List.of(reserveTicket))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data.id").value(55L))
                .andExpect(jsonPath("$.data.status").value(OrderStatus.WAITING_FOR_PAYMENT.name()));
    }

    @Test
    void cancelReservation_shouldReturnSuccessResponse() throws Exception {
        when(orderService.cancelReservation(42L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/orders/reservation/cancel")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void createPayment_shouldReturnPaypalOrder() throws Exception {
        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-9");

        when(orderService.createPayment(eq(42L), any(CreatePaymentDto.class))).thenReturn(paypalOrder);

        mockMvc.perform(post("/api/v1/orders/reservation/payment")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreatePaymentDto.builder()
                                .orderId(500L)
                                .cancelUrl("https://cancel")
                                .returnUrl("https://return")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value("paypal-order-9"));
    }

    @Test
    void cancelReservationById_shouldReturnSuccessResponse() throws Exception {
        when(orderService.cancelReservation(42L, 55L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/orders/55/reservation/cancel")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getByShowId_shouldReturnOrdersForAuthorizedUser() throws Exception {
        var order = Order.builder()
                .id(55L)
                .status(OrderStatus.FULFILLED)
                .placeTotal(50000.0)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(orderService.getByShowId(eq(42L), eq(77L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders/shows/77")
                        .requestAttr("sub", "42")
                        .param("from", "2026-05-15T00:00:00")
                        .param("to", "2026-05-16T00:00:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(55L));
    }

    @Test
    void getByShowIdAll_shouldReturnOrdersForAuthorizedUser() throws Exception {
        var order = Order.builder()
                .id(61L)
                .status(OrderStatus.FULFILLED)
                .placeTotal(75000.0)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(orderService.getByShowId(eq(42L), eq(77L))).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders/shows/77/all")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(61L));
    }

    @Test
    void handlePaypalWebhookCheckout_shouldProcessApprovedCheckoutOrder() throws Exception {
        when(payPalService.verifyWebhook(
                any(),
                eq("tx-id"),
                eq("2026-05-16T00:00:00Z"),
                eq("sig"),
                eq("cert"),
                eq("algo"),
                eq("checkout-webhook"))).thenReturn(true);
        doNothing().when(orderService).processPayment(42L, "paypal-order-1");

        String payload = """
                {
                  "event_type": "CHECKOUT.ORDER.APPROVED",
                  "resource": {
                    "id": "paypal-order-1",
                    "status": "APPROVED",
                    "purchase_units": [
                      {
                        "custom_id": "42"
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/orders/paypal/webhook/checkout")
                        .header("Paypal-Transmission-Id", "tx-id")
                        .header("Paypal-Transmission-Time", "2026-05-16T00:00:00Z")
                        .header("Paypal-Transmission-Sig", "sig")
                        .header("Paypal-Cert-Url", "cert")
                        .header("Paypal-Auth-Algo", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY));

        verify(orderService).processPayment(42L, "paypal-order-1");
    }

    @Test
    void handlePaypalWebhookCheckout_shouldRejectInvalidWebhook() throws Exception {
        when(payPalService.verifyWebhook(
                any(),
                eq("tx-id"),
                eq("2026-05-16T00:00:00Z"),
                eq("sig"),
                eq("cert"),
                eq("algo"),
                eq("checkout-webhook"))).thenReturn(false);

        mockMvc.perform(post("/api/v1/orders/paypal/webhook/checkout")
                        .header("Paypal-Transmission-Id", "tx-id")
                        .header("Paypal-Transmission-Time", "2026-05-16T00:00:00Z")
                        .header("Paypal-Transmission-Sig", "sig")
                        .header("Paypal-Cert-Url", "cert")
                        .header("Paypal-Auth-Algo", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(Constant.ErrorCode.INVALID_PAYPAL_WEBHOOK));

        verifyNoInteractions(orderService);
    }

    @Test
    void handlePaypalWebhookPayment_shouldRefundExpiredOrder() throws Exception {
        var order = Order.builder()
                .id(500L)
                .status(OrderStatus.APPROVED)
                .placeTotal(100000.0)
                .expiredAt(LocalDateTime.now().minusMinutes(5))
                .build();

        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-2");
        paypalOrder.setStatus(com.paypal.sdk.models.OrderStatus.COMPLETED);
        var purchaseUnit = new com.paypal.sdk.models.PurchaseUnit();
        purchaseUnit.setCustomId("500");
        paypalOrder.setPurchaseUnits(List.of(purchaseUnit));

        when(payPalService.verifyWebhook(
                any(),
                eq("tx-id"),
                eq("2026-05-16T00:00:00Z"),
                eq("sig"),
                eq("cert"),
                eq("algo"),
                eq("payment-webhook"))).thenReturn(true);
        when(payPalService.getOrderById("paypal-order-2"))
                .thenReturn(new ApiResponse<>(200, null, paypalOrder));
        when(orderService.findById(500L)).thenReturn(order);
        when(paymentService.fulfillPaymentFromPaypalOrder(any(PaymentWebhookDto.class), any()))
                .thenReturn(Payment.builder().id(1L).build());
        doNothing().when(orderService).refund(eq(order), eq("capture-2"));

        String payload = """
                {
                  "event_type": "PAYMENT.CAPTURE.COMPLETED",
                  "resource": {
                    "id": "capture-2",
                    "status": "COMPLETED",
                    "final_capture": true,
                    "supplementary_data": {
                      "related_ids": {
                        "order_id": "paypal-order-2"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/orders/paypal/webhook/payment")
                        .header("Paypal-Transmission-Id", "tx-id")
                        .header("Paypal-Transmission-Time", "2026-05-16T00:00:00Z")
                        .header("Paypal-Transmission-Sig", "sig")
                        .header("Paypal-Cert-Url", "cert")
                        .header("Paypal-Auth-Algo", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY));

        verify(paymentService).fulfillPaymentFromPaypalOrder(any(PaymentWebhookDto.class), eq(paypalOrder));
        verify(orderService).refund(eq(order), eq("capture-2"));
    }

    @Test
    void handlePaypalWebhookPayment_shouldFulfillActiveOrder() throws Exception {
        var order = Order.builder()
                .id(500L)
                .status(OrderStatus.APPROVED)
                .placeTotal(100000.0)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();

        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-4");
        paypalOrder.setStatus(com.paypal.sdk.models.OrderStatus.COMPLETED);
        var purchaseUnit = new com.paypal.sdk.models.PurchaseUnit();
        purchaseUnit.setCustomId("500");
        paypalOrder.setPurchaseUnits(List.of(purchaseUnit));

        when(payPalService.verifyWebhook(
                any(),
                eq("tx-id"),
                eq("2026-05-16T00:00:00Z"),
                eq("sig"),
                eq("cert"),
                eq("algo"),
                eq("payment-webhook"))).thenReturn(true);
        when(payPalService.getOrderById("paypal-order-4"))
                .thenReturn(new ApiResponse<>(200, null, paypalOrder));
        when(orderService.findById(500L)).thenReturn(order);
        when(paymentService.fulfillPaymentFromPaypalOrder(any(PaymentWebhookDto.class), any()))
                .thenReturn(Payment.builder().id(2L).build());
        when(orderService.fulfill(order)).thenReturn(order);
        doNothing().when(orderService).onOrderFulfilled(order);

        String payload = """
                {
                  "event_type": "PAYMENT.CAPTURE.COMPLETED",
                  "resource": {
                    "id": "capture-4",
                    "status": "COMPLETED",
                    "final_capture": true,
                    "supplementary_data": {
                      "related_ids": {
                        "order_id": "paypal-order-4"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/orders/paypal/webhook/payment")
                        .header("Paypal-Transmission-Id", "tx-id")
                        .header("Paypal-Transmission-Time", "2026-05-16T00:00:00Z")
                        .header("Paypal-Transmission-Sig", "sig")
                        .header("Paypal-Cert-Url", "cert")
                        .header("Paypal-Auth-Algo", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY));

        verify(paymentService).fulfillPaymentFromPaypalOrder(any(PaymentWebhookDto.class), eq(paypalOrder));
        verify(orderService).findById(500L);
        verify(orderService).fulfill(order);
        verify(orderService).onOrderFulfilled(order);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void handlePaypalWebhookPayment_shouldSkipAlreadyProcessedOrder() throws Exception {
        var order = Order.builder()
                .id(500L)
                .status(OrderStatus.FULFILLED)
                .placeTotal(100000.0)
                .expiredAt(LocalDateTime.now().minusMinutes(5))
                .build();

        var paypalOrder = new com.paypal.sdk.models.Order();
        paypalOrder.setId("paypal-order-3");
        paypalOrder.setStatus(com.paypal.sdk.models.OrderStatus.COMPLETED);
        var purchaseUnit = new com.paypal.sdk.models.PurchaseUnit();
        purchaseUnit.setCustomId("500");
        paypalOrder.setPurchaseUnits(List.of(purchaseUnit));

        when(payPalService.verifyWebhook(
                any(),
                eq("tx-id"),
                eq("2026-05-16T00:00:00Z"),
                eq("sig"),
                eq("cert"),
                eq("algo"),
                eq("payment-webhook"))).thenReturn(true);
        when(payPalService.getOrderById("paypal-order-3"))
                .thenReturn(new ApiResponse<>(200, null, paypalOrder));
        when(orderService.findById(500L)).thenReturn(order);

        String payload = """
                {
                  "event_type": "PAYMENT.CAPTURE.COMPLETED",
                  "resource": {
                    "id": "capture-3",
                    "status": "COMPLETED",
                    "final_capture": true,
                    "supplementary_data": {
                      "related_ids": {
                        "order_id": "paypal-order-3"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/orders/paypal/webhook/payment")
                        .header("Paypal-Transmission-Id", "tx-id")
                        .header("Paypal-Transmission-Time", "2026-05-16T00:00:00Z")
                        .header("Paypal-Transmission-Sig", "sig")
                        .header("Paypal-Cert-Url", "cert")
                        .header("Paypal-Auth-Algo", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY));

        verifyNoInteractions(paymentService);
        verify(orderService).findById(500L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void handlePaypalWebhookPayment_shouldRejectInvalidWebhook() throws Exception {
        when(payPalService.verifyWebhook(
                any(),
                eq("tx-id"),
                eq("2026-05-16T00:00:00Z"),
                eq("sig"),
                eq("cert"),
                eq("algo"),
                eq("payment-webhook"))).thenReturn(false);

        mockMvc.perform(post("/api/v1/orders/paypal/webhook/payment")
                        .header("Paypal-Transmission-Id", "tx-id")
                        .header("Paypal-Transmission-Time", "2026-05-16T00:00:00Z")
                        .header("Paypal-Transmission-Sig", "sig")
                        .header("Paypal-Cert-Url", "cert")
                        .header("Paypal-Auth-Algo", "algo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(Constant.ErrorCode.INVALID_PAYPAL_WEBHOOK));

        verifyNoInteractions(orderService, paymentService);
    }
}
