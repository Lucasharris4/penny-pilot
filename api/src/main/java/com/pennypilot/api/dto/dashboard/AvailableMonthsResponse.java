package com.pennypilot.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "List of months that have transaction data")
public record AvailableMonthsResponse(
        @Schema(description = "Months in YYYY-MM format, descending")
        List<String> months
) {}
