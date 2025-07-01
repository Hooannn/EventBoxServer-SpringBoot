package com.ht.eventbox.modules.auth.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthenticateDto {
    @NotEmpty(message = Constant.ValidationCode.TOKEN_NOT_EMPTY)
    @JsonProperty("access_token")
    private String accessToken;
}
