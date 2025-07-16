package com.ht.eventbox.modules.order.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationDto {
    @Getter
    @Setter
    public static class ReserveTicketDto {
        @NotNull
        @JsonProperty("ticket_id")
        private Long ticketId;

        @NotNull
        private int quantity;
    }

    @NotEmpty
    List<ReserveTicketDto> tickets = new ArrayList<>();
}
