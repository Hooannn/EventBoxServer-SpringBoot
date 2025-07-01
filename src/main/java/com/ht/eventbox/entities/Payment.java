package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.List;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;

    @Column(name = "paypal_order_id")
    @JsonProperty("paypal_order_id")
    private String paypalOrderId;

    @Column(name = "payer_email", nullable = false)
    @JsonProperty("payer_email")
    private String payerEmail;

    @Column(name = "payer_given_name", nullable = false)
    @JsonProperty("payer_given_name")
    private String payerGivenName;

    @Column(name = "payer_surname", nullable = false)
    @JsonProperty("payer_surname")
    private String payerSurname;

    @Column(name = "payer_id", nullable = false)
    @JsonProperty("payer_id")
    private String payerId;

    @Column(name = "paypal_capture_id")
    @JsonProperty("paypal_capture_id")
    private String paypalCaptureId;

    @Column(name = "captured_at")
    @JsonProperty("captured_at")
    private java.time.LocalDateTime capturedAt;

    @Column(name = "is_fulfilled", nullable = false)
    @JsonProperty("is_fulfilled")
    private Boolean isFulfilled = false;

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
}
