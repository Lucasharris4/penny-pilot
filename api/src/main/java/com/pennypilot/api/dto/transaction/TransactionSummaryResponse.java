package com.pennypilot.api.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated spending summary by category")
public record TransactionSummaryResponse(
        @Schema(description = "Category ID (null for uncategorized)")
        Long categoryId,

        @Schema(description = "Category name (\"Other\" for uncategorized)")
        String categoryName,

        @Schema(description = "Category hex color")
        String categoryColor,

        @Schema(description = "Category icon")
        String categoryIcon,

        @Schema(description = "Total amount in cents for this category")
        long totalCents,

        @Schema(description = "Number of transactions in this category")
        int transactionCount
) {}
