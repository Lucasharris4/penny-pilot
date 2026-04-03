package com.pennypilot.api.provider;

public record ProviderAccount(
        String accountId,
        String accountName,
        Integer balanceCents
) {}
