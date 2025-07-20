package com.ht.eventbox.modules.order;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Payment;
import com.ht.eventbox.modules.order.dtos.PaymentWebhookDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Payment createFromOrderAndPaypalOrder(Order order, com.paypal.sdk.models.Order payPalOrder) {
        Payment payment = Payment.builder()
                .paypalOrderId(payPalOrder.getId())
                .payerEmail(payPalOrder.getPayer().getEmailAddress())
                .payerGivenName(payPalOrder.getPayer().getName().getGivenName())
                .payerSurname(payPalOrder.getPayer().getName().getSurname())
                .payerId(payPalOrder.getPayer().getPayerId())
                .order(order)
                .isFulfilled(false)
                .build();

        return paymentRepository.save(payment);
    }

    public Payment fulfillPaymentFromPaypalOrder(PaymentWebhookDto paymentWebhookDto, com.paypal.sdk.models.Order paypalOrder) {
        String captureId = paymentWebhookDto.getResource().getId();
        var payment = paymentRepository.findByPaypalOrderId(paypalOrder.getId()).orElseThrow(() ->
                new HttpException("Không tìm thấy thông tin thanh toán cho đơn hàng PayPal", HttpStatus.NOT_FOUND)
        );

        payment.setIsFulfilled(true);
        payment.setGrossAmountCurrency(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getGrossAmount().getCurrencyCode());
        payment.setGrossAmountValue(Double.valueOf(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getGrossAmount().getValue()));
        payment.setNetAmountCurrency(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getNetAmount().getCurrencyCode());
        payment.setNetAmountValue(Double.valueOf(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getNetAmount().getValue()));
        payment.setPaypalFeeCurrency(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getPaypalFee().getCurrencyCode());
        payment.setPaypalFeeValue(Double.valueOf(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getPaypalFee().getValue()));
        payment.setPaypalCaptureId(captureId);
        payment.setCapturedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }
}
