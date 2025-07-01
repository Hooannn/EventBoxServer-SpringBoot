package com.ht.elearning.user.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.elearning.user.Role;
import com.ht.elearning.user.validation.CreatableRole;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {
    @Min(value = 8, message = "Password must have at least 8 characters")
    private String password;

    private String firstName;

    private String lastName;

    @CreatableRole(message = "Invalid role, just 'PROVIDER' or 'USER' accepted")
    private Role role;

    @JsonProperty("avatar_url")
    private String avatarUrl;
}
