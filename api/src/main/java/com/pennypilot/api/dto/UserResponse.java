package com.pennypilot.api.dto;

import com.pennypilot.api.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "User response (no sensitive fields)")
public record UserResponse(
        @Schema(description = "User ID")
        Long id,

        @Schema(description = "User email address")
        String email,

        @Schema(description = "Account creation timestamp")
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
