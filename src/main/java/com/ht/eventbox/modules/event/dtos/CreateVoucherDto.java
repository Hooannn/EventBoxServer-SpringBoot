package com.ht.eventbox.modules.event.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherDto {
    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    @JsonProperty("discount_type")
    @NotNull
    private DiscountType discountType;

    @JsonProperty("discount_value")
    @NotNull
    private double discountValue;

    @JsonProperty("valid_from")
    @NotNull
    private LocalDateTime validFrom;

    @JsonProperty("valid_to")
    @NotNull
    private LocalDateTime validTo;

    @JsonProperty("usage_limit")
    private int usageLimit;

    @JsonProperty("per_user_limit")
    private int perUserLimit;

    @JsonProperty("is_active")
    @NotNull
    private boolean isActive;

    @JsonProperty("is_public")
    @NotNull
    private boolean isPublic;

    @JsonProperty("min_order_value")
    @NotNull
    private double minOrderValue;

    @JsonProperty("min_ticket_quantity")
    @NotNull
    private int minTicketQuantity;
}
