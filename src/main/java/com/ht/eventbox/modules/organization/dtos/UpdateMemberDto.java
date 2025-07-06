package com.ht.eventbox.modules.organization.dtos;

import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.enums.AddableRole;
import jakarta.validation.constraints.Email;
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
public class UpdateMemberDto {
    @NotBlank(message = Constant.ValidationCode.EMAIL_NOT_EMPTY)
    @Email(message = Constant.ValidationCode.EMAIL_MUST_BE_VALID)
    private String email;

    @NotNull(message = Constant.ValidationCode.ORGANIZATION_ROLE_NOT_EMPTY)
    private AddableRole role;
}
