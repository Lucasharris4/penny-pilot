package com.pennypilot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(
        @Schema(description = "User email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Password", example = "securepass123")
        @NotBlank(message = "Password is required")
        String password
) {}
