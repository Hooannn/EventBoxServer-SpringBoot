package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vouchers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "event_id"})
})
@Checks({
        @Check(constraints = "valid_from < valid_to"),
        @Check(constraints = "discount_value > 0"),
        @Check(constraints = "usage_limit >= 0"),
        @Check(constraints = "per_user_limit >= 0"),
        @Check(constraints = "min_order_value >= 0"),
        @Check(constraints = "min_ticket_quantity >= 0"),
})
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JsonProperty("discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    @JsonProperty("discount_value")
    private Double discountValue;

    @Column(name = "valid_from", nullable = false)
    @JsonProperty("valid_from")
    private java.time.LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    @JsonProperty("valid_to")
    private java.time.LocalDateTime validTo;

    @Column(name = "usage_limit")
    @JsonProperty("usage_limit")
    private int usageLimit;

    @Column(name = "per_user_limit")
    @JsonProperty("per_user_limit")
    private int perUserLimit;

    @Column(name = "is_active", nullable = false)
    @JsonProperty("is_active")
    private boolean isActive;

    @Column(name = "is_public", nullable = false)
    @JsonProperty("is_public")
    private boolean isPublic;

    @Column(name = "min_order_value")
    @JsonProperty("min_order_value")
    private Double minOrderValue;

    @Column(name = "min_ticket_quantity")
    @JsonProperty("min_ticket_quantity")
    private int minTicketQuantity;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
