package com.ht.eventbox.modules.order.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentDto {
    @NotNull
    @JsonProperty("order_id")
    private Long orderId;

    @NotNull
    @JsonProperty("return_url")
    private String returnUrl;

    @NotNull
    @JsonProperty("cancel_url")
    private String cancelUrl;
}
