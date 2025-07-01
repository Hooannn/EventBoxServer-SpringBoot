package com.ht.eventbox.modules.auth.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshDto {
    @NotEmpty(message = Constant.ValidationCode.TOKEN_NOT_EMPTY)
    @JsonProperty("refresh_token")
    private String token;
}
