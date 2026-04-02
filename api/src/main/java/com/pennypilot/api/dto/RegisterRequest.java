package com.pennypilot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Registration request")
public record RegisterRequest(
        @Schema(description = "User email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Password (minimum length configured server-side)", example = "securepass123")
        @NotBlank(message = "Password is required")
        String password
) {}
