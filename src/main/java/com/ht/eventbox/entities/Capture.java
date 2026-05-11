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
@Table(name = "captures")
public class Capture {
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

    @Column(name = "paypal_capture_id", nullable = false)
    @JsonProperty("paypal_capture_id")
    private String paypalCaptureId;

    @Column(name = "status", nullable = false)
    @JsonProperty("status")
    private String status;

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

    @Column(name = "amount")
    @JsonProperty("amount")
    private Double amountValue;

    @Column(name = "amount_currency")
    @JsonProperty("amount_currency")
    private String amountCurrency;

    @JsonProperty("custom_id")
    @Column(name = "custom_id")
    private String customId;

    @Column(name = "final_capture", nullable = false)
    @JsonProperty("final_capture")
    private Boolean finalCapture = false;
}
