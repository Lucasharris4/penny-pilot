package com.pennypilot.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response containing JWT token")
public record LoginResponse(
        @Schema(description = "JWT authentication token")
        String token
) {}
