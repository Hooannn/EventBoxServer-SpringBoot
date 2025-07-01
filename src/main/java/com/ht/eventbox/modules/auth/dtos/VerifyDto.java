package com.ht.eventbox.modules.auth.dtos;

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
public class VerifyDto {
    @NotEmpty(message = Constant.ValidationCode.OTP_NOT_EMPTY)
    private String otp;

    @NotEmpty(message = Constant.ValidationCode.EMAIL_NOT_EMPTY)
    @Email(message = Constant.ValidationCode.EMAIL_MUST_BE_VALID)
    private String username;
}
