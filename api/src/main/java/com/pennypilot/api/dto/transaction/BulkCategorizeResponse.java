package com.pennypilot.api.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bulk categorize response")
public record BulkCategorizeResponse(
        @Schema(description = "Number of transactions updated")
        int updated,

        @Schema(description = "Invalid transaction IDs (only present on error)")
        List<Long> invalidIds
) {
    public BulkCategorizeResponse(int updated) {
        this(updated, null);
    }
}
