package com.pennypilot.api.dto.provider;

public record ProviderAccount(
        String accountId,
        String accountName,
        Integer balanceCents
) {}
