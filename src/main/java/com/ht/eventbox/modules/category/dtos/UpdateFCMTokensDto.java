package com.ht.eventbox.modules.category.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFCMTokensDto {
    @NotBlank(message = Constant.ValidationCode.TOKEN_NOT_EMPTY)
    private String token;

    @NotNull(message = Constant.ValidationCode.PLATFORM_NOT_EMPTY)
    private Platform platform;
}
