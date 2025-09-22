package com.ht.eventbox.modules.ticket.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiveawayTicketItemDto {
    @JsonProperty("recipient_email")
    @NotBlank
    private String recipientEmail;

    @NotBlank
    private String password;
}
