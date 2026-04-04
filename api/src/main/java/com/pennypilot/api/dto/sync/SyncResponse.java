package com.pennypilot.api.dto.sync;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Sync result summary")
public record SyncResponse(
        @Schema(description = "Number of new transactions added")
        int transactionsAdded,

        @Schema(description = "Number of existing transactions updated")
        int transactionsUpdated,

        @Schema(description = "Number of duplicate transactions skipped")
        int transactionsSkipped,

        @Schema(description = "Updated account balance in cents")
        Integer accountBalanceCents,

        @Schema(description = "Sync timestamp")
        Instant syncedAt
) {}
