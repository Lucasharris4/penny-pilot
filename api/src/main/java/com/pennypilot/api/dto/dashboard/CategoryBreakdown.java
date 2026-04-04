package com.pennypilot.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Spending breakdown for a single category")
public record CategoryBreakdown(
        @Schema(description = "Category ID, null for uncategorized")
        Long categoryId,

        @Schema(description = "Category name")
        String categoryName,

        @Schema(description = "Category color hex code")
        String categoryColor,

        @Schema(description = "Total amount in cents for this category")
        long amountCents,

        @Schema(description = "Percentage of total expenses")
        double percentage
) {}
