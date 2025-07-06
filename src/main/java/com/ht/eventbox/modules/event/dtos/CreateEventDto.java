package com.ht.eventbox.modules.event.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
            @NotBlank
            private String name;

            private String description;

            @NotNull
            private Double price;

            @NotNull
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

        @NotEmpty
        @JsonProperty("ticket_type_inputs")
        private List<CreateTicketTypeDto> ticketTypeInputs = new ArrayList<>();
    }

    @NotNull
    @JsonProperty("organization_id")
    private Long organizationId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String address;

    @JsonProperty("place")
    @NotBlank
    private String placeName;

    @NotBlank
    @JsonProperty("background_base64")
    private String backgroundBase64;

    @NotBlank
    @JsonProperty("logo_base64")
    private String logoBase64;

    @NotEmpty
    @JsonProperty("category_ids")
    private List<Long> categoryIds = new ArrayList<>();

    @NotEmpty
    @JsonProperty("keywords")
    private List<String> keywords = new ArrayList<>();

    @NotEmpty
    @JsonProperty("show_inputs")
    private List<CreateShowDto> showInputs = new ArrayList<>();
}
