package com.ht.eventbox.modules.ticket.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTicketItemDto {
    @NotBlank
    private String token;

    @NotNull
    @JsonProperty("event_show_id")
    private Long eventShowId;
}
