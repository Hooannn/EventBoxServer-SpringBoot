package com.ht.eventbox.modules.category.dtos;

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
public class CreateCategoryDto {
    @NotBlank
    @JsonProperty("name_vi")
    private String nameVi;

    @NotBlank
    @JsonProperty("name_en")
    private String nameEn;

    @NotNull
    private boolean featured;
}
