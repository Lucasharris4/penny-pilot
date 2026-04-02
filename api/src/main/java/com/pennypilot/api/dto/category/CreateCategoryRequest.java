package com.pennypilot.api.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Create category request")
public record CreateCategoryRequest(
        @Schema(description = "Category name", example = "Groceries")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Icon (emoji or icon name)", example = "\uD83D\uDED2")
        String icon,

        @Schema(description = "Hex color for charts", example = "#4CAF50")
        String color
) {}
