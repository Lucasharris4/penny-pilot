package com.pennypilot.api.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update category rule request")
public record UpdateCategoryRuleRequest(
        @Schema(description = "Glob-style match pattern (case-insensitive)", example = "STARBUCKS*")
        @NotBlank(message = "Match pattern is required")
        String matchPattern,

        @Schema(description = "Category ID to assign on match")
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Schema(description = "Priority (higher wins on conflicts)", example = "1")
        @NotNull(message = "Priority is required")
        Integer priority
) {}
