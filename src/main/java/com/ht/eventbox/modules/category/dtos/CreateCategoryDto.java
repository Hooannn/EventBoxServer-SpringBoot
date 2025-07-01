package com.ht.eventbox.modules.category.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.Email;
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
    @NotEmpty
    private String slug;

    @NotEmpty
    @JsonProperty("name_vi")
    private String nameVi;

    @NotEmpty
    @JsonProperty("name_en")
    private String nameEn;
}
