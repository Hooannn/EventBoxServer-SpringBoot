package com.ht.elearning.user.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.elearning.user.Role;
import com.ht.elearning.user.validation.CreatableRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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
public class CreateUserDto {
    @NotEmpty(message = "Password must not be empty")
    @Min(value = 8, message = "Password must have at least 8 characters")
    private String password;

    @NotEmpty(message = "Email must not be empty")
    @Email(message = "Email must be valid")
    private String email;

    @NotEmpty(message = "First name must be not empty")
    private String firstName;

    @NotEmpty(message = "Last name must be not empty")
    private String lastName;

    @NotNull(message = "Role must not be null")
    @CreatableRole(message = "Invalid role, just 'PROVIDER' or 'USER' accepted")
    private Role role;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private boolean verified;
}
