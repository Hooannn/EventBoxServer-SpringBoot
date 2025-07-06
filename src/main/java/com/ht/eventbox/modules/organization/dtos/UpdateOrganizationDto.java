package com.ht.eventbox.modules.organization.dtos;

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
public class UpdateOrganizationDto {
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    @JsonProperty("paypal_account")
    @Email(message = Constant.ValidationCode.EMAIL_MUST_BE_VALID)
    private String paypalAccount;

    @JsonProperty("logo_base64")
    private String logoBase64;

    private String phone;
    private String website;

    @Email(message = Constant.ValidationCode.EMAIL_MUST_BE_VALID)
    private String email;

    @JsonProperty("remove_logo")
    private boolean removeLogo = false;
}
