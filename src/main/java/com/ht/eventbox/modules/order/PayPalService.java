package com.ht.eventbox.modules.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.modules.order.dtos.PayPalWebhookVerifyDto;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class PayPalService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(PayPalService.class);
    private final PaypalServerSdkClient paypalServerSdkClient;

    private final String PAYPAL_API = "https://api-m.sandbox.paypal.com";

    public boolean verifyWebhook(
            String rawJsonPayload,
            String transmissionId,
            String transmissionTime,
            String transmissionSig,
            String certUrl,
            String authAlgo,
            String webhookId
    ) {
        // 1. Lấy PayPal Access Token (OAuth2)
        logger.info("Verifying PayPal webhook with transmissionId: {}, transmissionTime: {}, certUrl: {}, authAlgo: {}, webhookId: {}",
                transmissionId, transmissionTime, certUrl, authAlgo, webhookId);
        logger.info("Raw JSON payload: {}", rawJsonPayload);
        String accessToken = null;
        try {
            accessToken = paypalServerSdkClient.getClientCredentialsAuth().fetchToken().getAccessToken();
        } catch (ApiException | IOException e) {
            logger.error("Error retrieving PayPal access token: {}", e.getMessage());
        }
        if (accessToken == null) return false;
        logger.info("Access token retrieved: {}", accessToken);

        // 2. Tạo request body
        PayPalWebhookVerifyDto request = new PayPalWebhookVerifyDto();
        request.setTransmissionId(transmissionId);
        request.setTransmissionTime(transmissionTime);
        request.setTransmissionSig(transmissionSig);
        request.setCertUrl(certUrl);
        request.setAuthAlgo(authAlgo);
        request.setWebhookId(webhookId);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode webhookEvent = null;
        try {
            webhookEvent = objectMapper.readTree(rawJsonPayload);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing raw JSON payload: {}", e.getMessage());
            return false;
        }
        request.setWebhookEvent(webhookEvent); // Payload nguyên bản (dạng String hoặc Map)
        logger.info("request.webhookEvent: {}", request.getWebhookEvent());

        // 3. Gọi PayPal API để verify
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<PayPalWebhookVerifyDto> httpEntity = new HttpEntity<>(request, headers);
        String verifyUrl = PAYPAL_API + "/v1/notifications/verify-webhook-signature";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    verifyUrl,
                    httpEntity,
                    String.class
            );

            // 4. Kiểm tra response
            logger.info("PayPal webhook verification statusCode: {} - response: {}", response.getStatusCode(), response.getBody());
            return response.getStatusCode() == HttpStatus.OK
                    && response.getBody().contains("\"verification_status\":\"SUCCESS\"");
        } catch (Exception e) {
            logger.error("Error verifying PayPal webhook: {}", e.getMessage());
            return false;
        }
    }

    public ApiResponse<Order> createOrder(
            String customId,
            Double totalAmount,
            String currency,
            String description,
            String cancelUrl,
            String successUrl
    ) throws IOException, ApiException {
        CreateOrderInput createOrderInput = new CreateOrderInput.Builder(
                null,
                new OrderRequest.Builder(
                        CheckoutPaymentIntent.CAPTURE,
                        Arrays.asList(
                                new PurchaseUnitRequest.Builder(
                                        new AmountWithBreakdown.Builder(
                                                currency,
                                                totalAmount.toString()
                                        ).build()
                                ).description(description).customId(customId).build()
                        )
                ).paymentSource(
                                new PaymentSource.Builder()
                                        .paypal(new PaypalWallet.Builder()
                                                .experienceContext(new PaypalWalletExperienceContext.Builder()
                                                        .returnUrl(successUrl)
                                                        .cancelUrl(cancelUrl)
                                                        .userAction(PaypalExperienceUserAction.PAY_NOW)
                                                        .shippingPreference(null)
                                                        .landingPage(null)
                                                        .paymentMethodPreference(null)
                                                        .brandName("EventBox")
                                                        .locale("vi-VN")
                                                        .build())
                                                .build())
                                        .build()
                        )
                        .build()
        )
                .prefer("return=minimal")
                .build();

        OrdersController ordersController = paypalServerSdkClient.getOrdersController();

        return ordersController.createOrder(createOrderInput);
    }

    public ApiResponse<Order> captureOrder(String orderId) throws IOException, ApiException {
        OrdersController ordersController = paypalServerSdkClient.getOrdersController();

        CaptureOrderInput captureOrderInput = new CaptureOrderInput.Builder(
                orderId,
                null
        ).build();

        return ordersController.captureOrder(captureOrderInput);
    }

    public ApiResponse<Order> getOrderById(String orderId) throws IOException, ApiException {
        OrdersController ordersController = paypalServerSdkClient.getOrdersController();

        GetOrderInput getOrderInput = new GetOrderInput.Builder(orderId).build();

        return ordersController.getOrder(getOrderInput);
    }
}