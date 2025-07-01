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
public class ResetPasswordDto {
    @NotEmpty(message = Constant.ValidationCode.PASSWORD_NOT_EMPTY)
    private String password;

    @NotEmpty(message = Constant.ValidationCode.EMAIL_NOT_EMPTY)
    @Email(message = Constant.ValidationCode.EMAIL_MUST_BE_VALID)
    private String username;

    @NotEmpty(message = Constant.ValidationCode.OTP_NOT_EMPTY)
    private String otp;
}
