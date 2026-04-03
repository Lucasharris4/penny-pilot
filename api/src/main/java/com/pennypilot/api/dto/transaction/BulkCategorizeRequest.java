package com.pennypilot.api.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Bulk categorize transactions request")
public record BulkCategorizeRequest(
        @Schema(description = "List of transaction IDs to categorize")
        @NotEmpty(message = "Transaction IDs are required")
        List<Long> transactionIds,

        @Schema(description = "Category ID to assign")
        @NotNull(message = "Category ID is required")
        Long categoryId
) {}
