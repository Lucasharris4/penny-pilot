package com.pennypilot.api.dto.account;

import com.pennypilot.api.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Account response")
public record AccountResponse(
        @Schema(description = "Account ID")
        Long id,

        @Schema(description = "Provider ID")
        Long providerId,

        @Schema(description = "Provider name")
        String providerName,

        @Schema(description = "Account ID from the provider")
        String providerAccountId,

        @Schema(description = "Account name")
        String accountName,

        @Schema(description = "Balance in cents")
        Integer balanceCents,

        @Schema(description = "Last sync timestamp")
        Instant lastSyncedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getProvider().getId(),
                account.getProvider().getName().name(),
                account.getProviderAccountId(),
                account.getAccountName(),
                account.getBalanceCents(),
                account.getLastSyncedAt()
        );
    }
}
