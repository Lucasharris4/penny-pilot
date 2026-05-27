package com.pennypilot.api.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk ignore response")
public record BulkIgnoreResponse(
        @Schema(description = "Number of transactions updated")
        int updated
) {}
