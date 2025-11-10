package com.ht.eventbox.modules.auth.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthenticateWithIdTokenDto {
    @NotBlank(message = Constant.ValidationCode.TOKEN_NOT_EMPTY)
    @JsonProperty("id_token")
    private String idToken;
}
