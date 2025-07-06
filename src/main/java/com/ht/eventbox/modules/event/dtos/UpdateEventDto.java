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
public class UpdateEventDto {
    @NotEmpty
    private String title;

    @NotEmpty
    private String description;

    @NotEmpty
    private String address;

    @JsonProperty("place")
    @NotEmpty
    private String placeName;

    @JsonProperty("background_base64")
    private String backgroundBase64;

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
    private List<CreateEventDto.CreateShowDto> showInputs = new ArrayList<>();
}
