package com.pennypilot.api.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Bulk ignore transactions request")
public record BulkIgnoreRequest(
        @Schema(description = "List of transaction IDs to update")
        @NotEmpty(message = "Transaction IDs are required")
        List<Long> ids,

        @Schema(description = "Whether to ignore (true) or un-ignore (false) the transactions")
        @NotNull(message = "Ignored status is required")
        Boolean ignored
) {}
