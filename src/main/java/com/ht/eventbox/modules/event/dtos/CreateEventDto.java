package com.ht.eventbox.modules.event.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventDto {
    @Data
    @Builder
    public static class CreateShowDto {
        @Data
        @Builder
        public static class CreateTicketTypeDto {
            @NotEmpty
            private String name;

            private String description;

            @NotEmpty
            private Double price;

            @NotEmpty
            @JsonProperty("initial_stock")
            private int initialStock;
        }

        @NotNull
        @JsonProperty("start_time")
        private LocalDateTime startTime;

        @NotNull
        @JsonProperty("end_time")
        private LocalDateTime endTime;

        @NotNull
        @JsonProperty("sale_start_time")
        private LocalDateTime saleStartTime;

        @NotNull
        @JsonProperty("sale_end_time")
        private LocalDateTime saleEndTime;

        @NotNull
        @JsonProperty("ticket_type_inputs")
        private List<CreateTicketTypeDto> ticketTypeInputs = new ArrayList<>();
    }

    @NotNull
    @JsonProperty("organization_id")
    private Long organizationId;

    @NotEmpty
    private String title;

    @NotEmpty
    private String description;

    @NotEmpty
    private String address;

    @JsonProperty("place")
    @NotEmpty
    private String placeName;

    @NotEmpty
    @JsonProperty("background_base64")
    private String backgroundBase64;

    @NotEmpty
    @JsonProperty("logo_base64")
    private String logoBase64;

    @NotNull
    @JsonProperty("category_ids")
    private List<Long> categoryIds = new ArrayList<>();

    @NotNull
    @JsonProperty("keywords")
    private List<String> keywords = new ArrayList<>();

    @NotNull
    @JsonProperty("show_inputs")
    private List<CreateShowDto> showInputs = new ArrayList<>();
}
