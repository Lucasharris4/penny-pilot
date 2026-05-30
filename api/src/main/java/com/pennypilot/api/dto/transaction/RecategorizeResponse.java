package com.pennypilot.api.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a recategorize-all operation")
public record RecategorizeResponse(
        @Schema(description = "Transactions processed through rules (had a merchant name)")
        int recalculated,

        @Schema(description = "Transactions whose category changed")
        int updated,

        @Schema(description = "Transactions skipped (no merchant name)")
        int skipped
) {}
