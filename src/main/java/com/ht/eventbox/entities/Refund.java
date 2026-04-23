package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    @JsonBackReference
    private Payment payment;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;

    @Column(name = "create_time")
    @JsonProperty("created_time")
    private String createTime;

    @Column(name = "update_time")
    @JsonProperty("update_time")
    private String updateTime;

    @Column(name = "paypal_refund_id", nullable = false)
    @JsonProperty("paypal_refund_id")
    private String paypalRefundId;

    @Column(name = "invoice_id")
    @JsonProperty("invoice_id")
    private String invoiceId;

    @Column(name = "status", nullable = false)
    @JsonProperty("status")
    private String status;

    @Column(name = "payer_email")
    @JsonProperty("payer_email")
    private String payerEmail;

    @Column(name = "payer_id")
    @JsonProperty("payer_id")
    private String payerMerchantId;

    @Column(name = "paypal_fee")
    @JsonProperty("paypal_fee")
    private Double paypalFeeValue;

    @Column(name = "paypal_fee_currency")
    @JsonProperty("paypal_fee_currency")
    private String paypalFeeCurrency;

    @Column(name = "gross_amount")
    @JsonProperty("gross_amount")
    private Double grossAmountValue;

    @Column(name = "gross_amount_currency")
    @JsonProperty("gross_amount_currency")
    private String grossAmountCurrency;

    @Column(name = "net_amount")
    @JsonProperty("net_amount")
    private Double netAmountValue;

    @Column(name = "net_amount_currency")
    @JsonProperty("net_amount_currency")
    private String netAmountCurrency;

    @Column(name = "total_refunded_amount")
    @JsonProperty("total_refunded_amount")
    private Double totalRefundedAmountValue;

    @Column(name = "total_refunded_amount_currency")
    @JsonProperty("total_refunded_amount_currency")
    private String totalRefundedAmountCurrency;

    @Column(name = "amount")
    @JsonProperty("amount")
    private Double amountValue;

    @Column(name = "amount_currency")
    @JsonProperty("amount_currency")
    private String amountCurrency;

    @Column(name = "note_to_payer")
    @JsonProperty("note_to_payer")
    private String noteToPayer;

    @JsonProperty("custom_id")
    @Column(name = "custom_id")
    private String customId;

    @JsonProperty("acquirer_reference_number")
    @Column(name = "acquirer_reference_number")
    private String acquirerReferenceNumber;
}
