package com.pennypilot.api.provider;

import com.pennypilot.api.entity.TransactionType;

import java.time.LocalDate;

public record ProviderTransaction(
        String transactionId,
        String accountId,
        Integer amountCents,
        TransactionType transactionType,
        String description,
        String merchantName,
        LocalDate date
) {}
