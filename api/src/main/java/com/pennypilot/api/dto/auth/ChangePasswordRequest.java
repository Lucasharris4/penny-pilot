package com.pennypilot.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Change password request")
public record ChangePasswordRequest(
        @Schema(description = "Current password")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "New password (minimum length configured server-side)")
        @NotBlank(message = "New password is required")
        String newPassword
) {}
