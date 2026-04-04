package com.pennypilot.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dashboard summary with totals and category breakdown")
public record DashboardSummaryResponse(
        @Schema(description = "Total income in cents")
        long incomeCents,

        @Schema(description = "Total expenses in cents")
        long expensesCents,

        @Schema(description = "Net cash flow in cents (income - expenses)")
        long netCents,

        @Schema(description = "Spending breakdown by category")
        List<CategoryBreakdown> byCategory
) {}
