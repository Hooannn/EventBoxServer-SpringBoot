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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


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
            @RequestHeader("Paypal-Auth-Algo") String authAlgo
    ) {
        boolean isValid = payPalService.verifyWebhook(
                rawJsonPayload,
                transmissionId,
                transmissionTime,
                transmissionSig,
                certUrl,
                authAlgo,
                checkoutWebhookId
        );

        logger.info("Verifying PayPal webhook: {}",
                isValid ? "Webhook hợp lệ" : "Webhook không hợp lệ"
        );

        if (!isValid)
            throw new HttpException("Webhook PayPal không hợp lệ", HttpStatus.BAD_REQUEST);

        ObjectMapper mapper = new ObjectMapper();
        CheckoutWebhookDto checkoutWebhookDto = null;
        try {
            checkoutWebhookDto = mapper.readValue(rawJsonPayload, CheckoutWebhookDto.class);
        } catch (JsonProcessingException e) {
            logger.error("Lỗi khi parse JSON payload: {}", e.getMessage());
        }

        if (checkoutWebhookDto != null && checkoutWebhookDto.getEventType().equals("CHECKOUT.ORDER.APPROVED") && checkoutWebhookDto.getResource().getStatus().equals("APPROVED")) {
            String customId = checkoutWebhookDto.getResource().getPurchaseUnits().get(0).getCustomId();
            Order order = orderService.findById(Long.parseLong(customId));
            try {
                var captureResponse = payPalService.captureOrder(
                        checkoutWebhookDto.getResource().getId()
                );
                if (captureResponse.getStatusCode() != 201 && captureResponse.getStatusCode() != 200) {
                    logger.error("Failed to capture PayPal order: {}", captureResponse);
                    throw new HttpException("Không thể capture đơn hàng PayPal", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                logger.info("Capture PayPal order successful: {}", captureResponse.getResult());

                var payPalorder = captureResponse.getResult();
                paymentService.createFromOrderAndPaypalOrder(order, payPalorder);
                if (order.getStatus() == OrderStatus.PENDING)
                    order.setStatus(OrderStatus.APPROVED);
                orderService.save(order);
            } catch (IOException | ApiException e) {
                throw new HttpException("Lỗi khi capture đơn hàng PayPal: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return ResponseEntity.ok().body(new Response<>(
                HttpStatus.OK.value(),
                "Webhook PayPal cho Checkout thành công",
                null
        ));
    }

    @PostMapping("/paypal/webhook/payment")
    public ResponseEntity<Response<String>> handlePaypalWebhookPayment(
            @RequestBody String rawJsonPayload,
            @RequestHeader("Paypal-Transmission-Id") String transmissionId,
            @RequestHeader("Paypal-Transmission-Time") String transmissionTime,
            @RequestHeader("Paypal-Transmission-Sig") String transmissionSig,
            @RequestHeader("Paypal-Cert-Url") String certUrl,
            @RequestHeader("Paypal-Auth-Algo") String authAlgo
    ) {
        boolean isValid = payPalService.verifyWebhook(
                rawJsonPayload,
                transmissionId,
                transmissionTime,
                transmissionSig,
                certUrl,
                authAlgo,
                paymentWebhookId
        );

        logger.info("Verifying PayPal webhook: {}",
                isValid ? "Webhook hợp lệ" : "Webhook không hợp lệ"
        );

        if (!isValid)
            throw new HttpException("Webhook PayPal không hợp lệ", HttpStatus.BAD_REQUEST);

        ObjectMapper mapper = new ObjectMapper();
        PaymentWebhookDto paymentWebhookDto = null;
        try {
            paymentWebhookDto = mapper.readValue(rawJsonPayload, PaymentWebhookDto.class);
        } catch (JsonProcessingException e) {
            logger.error("Lỗi khi parse JSON payload: {}", e.getMessage());
        }

        if (paymentWebhookDto != null && paymentWebhookDto.getEventType().equals("PAYMENT.CAPTURE.COMPLETED")
                && paymentWebhookDto.getResource().getStatus().equals("COMPLETED")
                && paymentWebhookDto.getResource().isFinalCapture()
        ) {
            var orderId = paymentWebhookDto.getResource().getSupplementaryData().getRelatedIds().getOrderId();

            try {
                var paypalOrderRes = payPalService.getOrderById(orderId);

                if (paypalOrderRes.getStatusCode() != 200 && paypalOrderRes.getStatusCode() != 201) {
                    throw new HttpException("Không thể lấy thông tin đơn hàng PayPal", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                logger.info("Get PayPal order successful: {}", paypalOrderRes.getResult());

                var paypalOrder = paypalOrderRes.getResult();
                paymentService.fulfillPaymentFromPaypalOrder(paymentWebhookDto, paypalOrder);
                var customId = paypalOrder.getPurchaseUnits().get(0).getCustomId();
                var order = orderService.fulfill(Long.parseLong(customId));
                orderService.onOrderFulfilled(order);
            } catch (IOException | ApiException e) {
                throw new HttpException("Error getting PayPal order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return ResponseEntity.ok().body(new Response<>(
                HttpStatus.OK.value(),
                "Webhook PayPal cho Checkout thành công",
                null
        ));
    }

    @PostMapping("/reservation")
    @RequiredPermissions({"create:orders"})
    public ResponseEntity<Response<Order>> createReservation(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody CreateReservationDto createReservationDto
    )
    {
        var res = orderService.createReservation(Long.valueOf(sub), createReservationDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    @PostMapping("/reservation/payment")
    @RequiredPermissions({"create:orders"})
    public ResponseEntity<Response<com.paypal.sdk.models.Order>> createPayment(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody CreatePaymentDto createPaymentDto
    )
    {
        var res = orderService.createPayment(Long.valueOf(sub), createPaymentDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping("/reservation/cancel")
    @RequiredPermissions({"create:orders"})
    public ResponseEntity<Response<Boolean>> cancelReservation(
            @RequestAttribute("sub") String sub
    )
    {
        var res = orderService.cancelReservation(Long.valueOf(sub));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }
}
