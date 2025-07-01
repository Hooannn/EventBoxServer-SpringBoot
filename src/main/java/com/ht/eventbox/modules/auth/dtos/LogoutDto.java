package com.ht.eventbox.modules.auth.dtos;

import com.ht.eventbox.constant.Constant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ht.eventbox.enums.Platform;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutDto {
    @NotNull(message = Constant.ValidationCode.PLATFORM_NOT_EMPTY)
    private Platform platform;
}
