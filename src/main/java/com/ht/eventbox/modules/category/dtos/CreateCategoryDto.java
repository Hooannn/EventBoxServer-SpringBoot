package com.ht.eventbox.modules.category.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
    private String slug;

    @NotBlank
    @JsonProperty("name_vi")
    private String nameVi;

    @NotBlank
    @JsonProperty("name_en")
    private String nameEn;
}
