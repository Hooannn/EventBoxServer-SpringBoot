package com.ht.eventbox.modules.order;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.entities.Capture;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Payment;
import com.ht.eventbox.modules.order.dtos.PaymentWebhookDto;
import com.paypal.sdk.models.OrdersCapture;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
        private final PaymentRepository paymentRepository;
        private final CaptureRepository captureRepository;

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

        public Payment fulfillPaymentFromPaypalOrder(PaymentWebhookDto paymentWebhookDto,
                        com.paypal.sdk.models.Order paypalOrder) {
                String captureId = paymentWebhookDto.getResource().getId();
                var payment = paymentRepository.findByPaypalOrderId(paypalOrder.getId())
                                .orElseThrow(() -> new HttpException(
                                                "Không tìm thấy thông tin thanh toán cho đơn hàng PayPal",
                                                HttpStatus.NOT_FOUND));

                payment.setIsFulfilled(true);
                payment.setGrossAmountCurrency(
                                paymentWebhookDto.getResource().getSellerReceivableBreakdown().getGrossAmount()
                                                .getCurrencyCode());
                payment.setGrossAmountValue(Double
                                .valueOf(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getGrossAmount()
                                                .getValue()));
                payment.setNetAmountCurrency(
                                paymentWebhookDto.getResource().getSellerReceivableBreakdown().getNetAmount()
                                                .getCurrencyCode());
                payment.setNetAmountValue(Double
                                .valueOf(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getNetAmount()
                                                .getValue()));
                payment.setPaypalFeeCurrency(
                                paymentWebhookDto.getResource().getSellerReceivableBreakdown().getPaypalFee()
                                                .getCurrencyCode());
                payment.setPaypalFeeValue(Double
                                .valueOf(paymentWebhookDto.getResource().getSellerReceivableBreakdown().getPaypalFee()
                                                .getValue()));
                payment.setPaypalCaptureId(captureId);
                payment.setCapturedAt(LocalDateTime.now());

                createCaptureRecord(payment, paymentWebhookDto);

                return paymentRepository.save(payment);
        }

        public Payment fulfillPaymentFromPaypalOrder(OrdersCapture paypalOrderCapture,
                        com.paypal.sdk.models.Order paypalOrder) {
                String captureId = paypalOrderCapture.getId();

                var payment = paymentRepository.findByPaypalOrderId(paypalOrder.getId())
                                .orElseThrow(() -> new HttpException(
                                                "Không tìm thấy thông tin thanh toán cho đơn hàng PayPal",
                                                HttpStatus.NOT_FOUND));

                payment.setIsFulfilled(true);
                payment.setGrossAmountCurrency(
                                paypalOrderCapture.getSellerReceivableBreakdown().getGrossAmount().getCurrencyCode());
                payment.setGrossAmountValue(Double
                                .valueOf(paypalOrderCapture.getSellerReceivableBreakdown().getGrossAmount()
                                                .getValue()));
                payment.setNetAmountCurrency(
                                paypalOrderCapture.getSellerReceivableBreakdown().getNetAmount().getCurrencyCode());
                payment.setNetAmountValue(Double
                                .valueOf(paypalOrderCapture.getSellerReceivableBreakdown().getNetAmount().getValue()));
                payment.setPaypalFeeCurrency(
                                paypalOrderCapture.getSellerReceivableBreakdown().getPaypalFee().getCurrencyCode());
                payment.setPaypalFeeValue(Double
                                .valueOf(paypalOrderCapture.getSellerReceivableBreakdown().getPaypalFee().getValue()));
                payment.setPaypalCaptureId(captureId);
                payment.setCapturedAt(LocalDateTime.now());

                createCaptureRecord(payment, paypalOrderCapture);

                return paymentRepository.save(payment);
        }

        public Capture createCaptureRecord(Payment payment, PaymentWebhookDto paymentWebhookDto) {
                // todo: parse các trường còn thiếu từ paymentWebhookDto để lưu vào Capture
                Capture capture = Capture.builder()
                                .paypalCaptureId(paymentWebhookDto.getResource().getId())
                                .payment(payment)
                                // .amountCurrency(paymentWebhookDto.getResource().getSellerReceivableBreakdown()
                                // .getAmount().getCurrencyCode())
                                // .amountValue(Double.valueOf(paymentWebhookDto.getResource()
                                // .getSellerReceivableBreakdown().getAmount().getValue()))
                                .grossAmountCurrency(
                                                paymentWebhookDto.getResource().getSellerReceivableBreakdown()
                                                                .getGrossAmount()
                                                                .getCurrencyCode())
                                .grossAmountValue(
                                                Double.valueOf(paymentWebhookDto.getResource()
                                                                .getSellerReceivableBreakdown().getGrossAmount()
                                                                .getValue()))
                                .netAmountCurrency(
                                                paymentWebhookDto.getResource().getSellerReceivableBreakdown()
                                                                .getNetAmount().getCurrencyCode())
                                .netAmountValue(
                                                Double.valueOf(paymentWebhookDto.getResource()
                                                                .getSellerReceivableBreakdown().getNetAmount()
                                                                .getValue()))
                                .paypalFeeCurrency(
                                                paymentWebhookDto.getResource().getSellerReceivableBreakdown()
                                                                .getPaypalFee().getCurrencyCode())
                                .paypalFeeValue(
                                                Double.valueOf(paymentWebhookDto.getResource()
                                                                .getSellerReceivableBreakdown().getPaypalFee()
                                                                .getValue()))
                                // .customId(paymentWebhookDto.getResource().getCustomId())
                                // .createTime(paymentWebhookDto.getResource().getCreateTime())
                                // .updateTime(paymentWebhookDto.getResource().getUpdateTime())
                                .status(paymentWebhookDto.getResource().getStatus().toString())
                                // .finalCapture(paymentWebhookDto.getResource().getFinalCapture())
                                .build();

                return captureRepository.save(capture);
        }

        public Capture createCaptureRecord(Payment payment, OrdersCapture paypalOrderCapture) {
                Capture capture = Capture.builder()
                                .paypalCaptureId(paypalOrderCapture.getId())
                                .payment(payment)
                                .amountCurrency(paypalOrderCapture.getAmount().getCurrencyCode())
                                .amountValue(Double.valueOf(paypalOrderCapture.getAmount().getValue()))
                                .grossAmountCurrency(
                                                paypalOrderCapture.getSellerReceivableBreakdown().getGrossAmount()
                                                                .getCurrencyCode())
                                .grossAmountValue(
                                                Double.valueOf(paypalOrderCapture.getSellerReceivableBreakdown()
                                                                .getGrossAmount().getValue()))
                                .netAmountCurrency(paypalOrderCapture.getSellerReceivableBreakdown().getNetAmount()
                                                .getCurrencyCode())
                                .netAmountValue(
                                                Double.valueOf(paypalOrderCapture.getSellerReceivableBreakdown()
                                                                .getNetAmount().getValue()))
                                .paypalFeeCurrency(paypalOrderCapture.getSellerReceivableBreakdown().getPaypalFee()
                                                .getCurrencyCode())
                                .paypalFeeValue(
                                                Double.valueOf(paypalOrderCapture.getSellerReceivableBreakdown()
                                                                .getPaypalFee().getValue()))
                                .customId(paypalOrderCapture.getCustomId())
                                .createTime(paypalOrderCapture.getCreateTime())
                                .updateTime(paypalOrderCapture.getUpdateTime())
                                .status(paypalOrderCapture.getStatus().toString())
                                .finalCapture(paypalOrderCapture.getFinalCapture())
                                .build();

                return captureRepository.save(capture);
        }
}
