package com.ht.eventbox.modules.auth.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {
    @NotBlank(message = Constant.ValidationCode.PASSWORD_NOT_EMPTY)
    private String password;

    @NotBlank(message = Constant.ValidationCode.EMAIL_NOT_EMPTY)
    @Email(message = Constant.ValidationCode.EMAIL_MUST_BE_VALID)
    private String username;

    @NotBlank(message = Constant.ValidationCode.FIRST_NAME_NOT_EMPTY)
    @JsonProperty("first_name")
    private String firstName;

    @NotBlank(message = Constant.ValidationCode.LAST_NAME_NOT_EMPTY)
    @JsonProperty("last_name")
    private String lastName;
}
