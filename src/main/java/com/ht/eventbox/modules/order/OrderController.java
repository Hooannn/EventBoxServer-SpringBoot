package com.ht.eventbox.modules.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.order.dtos.CheckoutWebhookDto;
import com.ht.eventbox.modules.order.dtos.CreatePaymentDto;
import com.ht.eventbox.modules.order.dtos.CreateReservationDto;
import com.ht.eventbox.modules.order.dtos.PaymentWebhookDto;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
        private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OrderController.class);

        @Value("${paypal.checkout.webhook.id}")
        private String checkoutWebhookId;

        @Value("${paypal.payment.webhook.id}")
        private String paymentWebhookId;

        private final OrderService orderService;
        private final PayPalService payPalService;
        private final PaymentService paymentService;

        @PostMapping("/paypal/webhook/checkout")
        public ResponseEntity<Response<String>> handlePaypalWebhookCheckout(
                        @RequestBody String rawJsonPayload,
                        @RequestHeader("Paypal-Transmission-Id") String transmissionId,
                        @RequestHeader("Paypal-Transmission-Time") String transmissionTime,
                        @RequestHeader("Paypal-Transmission-Sig") String transmissionSig,
                        @RequestHeader("Paypal-Cert-Url") String certUrl,
                        @RequestHeader("Paypal-Auth-Algo") String authAlgo) {
                boolean isValid = payPalService.verifyWebhook(
                                rawJsonPayload,
                                transmissionId,
                                transmissionTime,
                                transmissionSig,
                                certUrl,
                                authAlgo,
                                checkoutWebhookId);

                if (!isValid)
                        throw new HttpException(Constant.ErrorCode.INVALID_PAYPAL_WEBHOOK, HttpStatus.BAD_REQUEST);

                ObjectMapper mapper = new ObjectMapper();
                CheckoutWebhookDto checkoutWebhookDto = null;
                try {
                        checkoutWebhookDto = mapper.readValue(rawJsonPayload, CheckoutWebhookDto.class);
                } catch (JsonProcessingException e) {
                        logger.error("Error parsing JSON payload: {}", e.getMessage());
                }

                if (checkoutWebhookDto != null
                                && checkoutWebhookDto.getEventType()
                                                .equals(Constant.WebhookEvent.CHECKOUT_ORDER_APPROVED)
                                && checkoutWebhookDto.getResource().getStatus()
                                                .equals(Constant.WebhookEvent.CHECKOUT_ORDER_APPROVED_STATUS)) {

                        String customId = checkoutWebhookDto.getResource().getPurchaseUnits().get(0).getCustomId();

                        orderService.processPayment(Long.parseLong(customId), checkoutWebhookDto.getResource().getId());

                }

                return ResponseEntity.ok().body(new Response<>(
                                HttpStatus.OK.value(),
                                Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY,
                                null));
        }

        @PostMapping("/paypal/webhook/payment")
        public ResponseEntity<Response<String>> handlePaypalWebhookPayment(
                        @RequestBody String rawJsonPayload,
                        @RequestHeader("Paypal-Transmission-Id") String transmissionId,
                        @RequestHeader("Paypal-Transmission-Time") String transmissionTime,
                        @RequestHeader("Paypal-Transmission-Sig") String transmissionSig,
                        @RequestHeader("Paypal-Cert-Url") String certUrl,
                        @RequestHeader("Paypal-Auth-Algo") String authAlgo) {
                boolean isValid = payPalService.verifyWebhook(
                                rawJsonPayload,
                                transmissionId,
                                transmissionTime,
                                transmissionSig,
                                certUrl,
                                authAlgo,
                                paymentWebhookId);

                if (!isValid)
                        throw new HttpException(Constant.ErrorCode.INVALID_PAYPAL_WEBHOOK, HttpStatus.BAD_REQUEST);

                ObjectMapper mapper = new ObjectMapper();
                PaymentWebhookDto paymentWebhookDto = null;
                try {
                        paymentWebhookDto = mapper.readValue(rawJsonPayload, PaymentWebhookDto.class);
                } catch (JsonProcessingException e) {
                        logger.error("Error parsing JSON payload: {}", e.getMessage());
                }

                if (paymentWebhookDto != null
                                && paymentWebhookDto.getEventType()
                                                .equals(Constant.WebhookEvent.PAYMENT_CAPTURE_COMPLETED)
                                && paymentWebhookDto.getResource().getStatus()
                                                .equals(Constant.WebhookEvent.PAYMENT_CAPTURE_COMPLETED_STATUS)
                                && paymentWebhookDto.getResource().isFinalCapture()) {
                        var orderId = paymentWebhookDto.getResource().getSupplementaryData().getRelatedIds()
                                        .getOrderId();

                        ApiResponse<com.paypal.sdk.models.Order> paypalOrderRes = null;
                        try {
                                paypalOrderRes = payPalService.getOrderById(orderId);
                        } catch (ApiException | IOException e) {
                                throw new HttpException(
                                                Constant.ErrorCode.INVALID_PAYPAL_ORDER,
                                                HttpStatus.INTERNAL_SERVER_ERROR);
                        }

                        if (paypalOrderRes.getStatusCode() != 200 && paypalOrderRes.getStatusCode() != 201) {
                                throw new HttpException(
                                                Constant.ErrorCode.INVALID_PAYPAL_ORDER,
                                                HttpStatus.INTERNAL_SERVER_ERROR);
                        }

                        var paypalOrder = paypalOrderRes.getResult();
                        var captureId = paymentWebhookDto.getResource().getId();
                        var customId = paypalOrder.getPurchaseUnits().get(0).getCustomId();
                        var order = orderService.findById(Long.parseLong(customId));

                        if (order.getStatus() == OrderStatus.FULFILLED || order.getStatus() == OrderStatus.PENDING) {
                                logger.info("Order #{} has already been processed, skipping payment processing",
                                                order.getId());

                                return ResponseEntity.ok().body(new Response<>(
                                                HttpStatus.OK.value(),
                                                Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY,
                                                null));
                        }

                        paymentService.fulfillPaymentFromPaypalOrder(paymentWebhookDto, paypalOrder);

                        if (order.getExpiredAt().isBefore(LocalDateTime.now())) {
                                orderService.refund(order, captureId);
                        } else {
                                var fulfilledOrder = orderService.fulfill(order);
                                orderService.onOrderFulfilled(fulfilledOrder);
                        }

                }

                return ResponseEntity.ok().body(new Response<>(
                                HttpStatus.OK.value(),
                                Constant.SuccessCode.PAYPAL_WEBHOOK_HANDLE_SUCCESSFULLY,
                                null));
        }

        @PostMapping("/reservation")
        @RequiredPermissions({ "create:orders" })
        public ResponseEntity<Response<Order>> createReservation(
                        @RequestAttribute("sub") String sub,
                        @Valid @RequestBody CreateReservationDto createReservationDto) {
                var res = orderService.createReservation(Long.valueOf(sub), createReservationDto);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                                                res));
        }

        @PostMapping("/reservation/payment")
        @RequiredPermissions({ "create:orders" })
        public ResponseEntity<Response<com.paypal.sdk.models.Order>> createPayment(
                        @RequestAttribute("sub") String sub,
                        @Valid @RequestBody CreatePaymentDto createPaymentDto) {
                var res = orderService.createPayment(Long.valueOf(sub), createPaymentDto);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        @PostMapping("/{orderId}/reservation/cancel")
        @RequiredPermissions({ "create:orders" })
        public ResponseEntity<Response<Boolean>> cancelReservationById(
                        @RequestAttribute("sub") String sub,
                        @PathVariable Long orderId) {
                var res = orderService.cancelReservation(Long.valueOf(sub), orderId);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                                                res));
        }

        @PostMapping("/reservation/cancel")
        @RequiredPermissions({ "create:orders" })
        public ResponseEntity<Response<Boolean>> cancelReservation(
                        @RequestAttribute("sub") String sub) {
                var res = orderService.cancelReservation(Long.valueOf(sub));
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                                                res));
        }

        /*
         * API dùng để lấy tất cả đơn hàng theo mã chương trình showId trong khoảng thời
         * gian từ "from" đến "to", dùng cho web ban tổ chức khi xem báo cáo
         */
        @GetMapping("/shows/{showId}")
        @RequiredPermissions({ "read:orders" })
        public ResponseEntity<Response<List<Order>>> getByShowId(
                        @RequestAttribute("sub") String sub,
                        @RequestParam(value = "from") LocalDateTime from,
                        @RequestParam(value = "to") LocalDateTime to,
                        @PathVariable Long showId) {
                var res = orderService.getByShowId(Long.valueOf(sub), showId, from, to);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        /*
         * API dùng để lấy tất cả đơn hàng theo mã chương trình showId, dùng cho web ban
         * tổ chức khi xem báo cáo
         */
        @GetMapping("/shows/{showId}/all")
        @RequiredPermissions({ "read:orders" })
        public ResponseEntity<Response<List<Order>>> getByShowId(
                        @RequestAttribute("sub") String sub,
                        @PathVariable Long showId) {
                var res = orderService.getByShowId(Long.valueOf(sub), showId);
                return ResponseEntity.ok(
                                new Response<>(
                                                HttpStatus.OK.value(),
                                                HttpStatus.OK.getReasonPhrase(),
                                                res));
        }

        @GetMapping("/paypal/orders/{orderId}")
        @RequiredPermissions({ "read:paypal_orders", "access:admin" })
        public ResponseEntity<Response<com.paypal.sdk.models.Order>> getPaypalOrderById(
                        @RequestAttribute("sub") String sub,
                        @PathVariable String orderId) {
                try {
                        var res = payPalService.getOrderById(orderId);
                        if (res.getStatusCode() != 200 && res.getStatusCode() != 201) {
                                throw new HttpException(
                                                Constant.ErrorCode.INTERNAL_SERVER_ERROR,
                                                HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        return ResponseEntity.ok(
                                        new Response<>(
                                                        HttpStatus.OK.value(),
                                                        HttpStatus.OK.getReasonPhrase(),
                                                        res.getResult()));
                } catch (IOException | ApiException e) {
                        throw new HttpException(e.getMessage(),
                                        HttpStatus.INTERNAL_SERVER_ERROR);
                }

        }
}
