package com.pennypilot.api.dto.transaction;

import com.pennypilot.api.entity.Transaction;
import com.pennypilot.api.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transaction response")
public record TransactionResponse(
        @Schema(description = "Transaction ID")
        Long id,

        @Schema(description = "Account ID")
        Long accountId,

        @Schema(description = "Category ID (null if uncategorized)")
        Long categoryId,

        @Schema(description = "Category name (\"Other\" if uncategorized)")
        String categoryName,

        @Schema(description = "Amount in cents (unsigned, always positive)")
        Integer amountCents,

        @Schema(description = "Transaction type: CREDIT or DEBIT")
        TransactionType transactionType,

        @Schema(description = "Transaction description")
        String description,

        @Schema(description = "Normalized merchant name")
        String merchantName,

        @Schema(description = "ISO 8601 date")
        String date,

        @Schema(description = "External ID from provider (for deduplication)")
        String externalId
) {
    public static TransactionResponse from(Transaction transaction, String categoryName) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getCategoryId(),
                categoryName,
                transaction.getAmountCents(),
                transaction.getTransactionType(),
                transaction.getDescription(),
                transaction.getMerchantName(),
                transaction.getDate(),
                transaction.getExternalId()
        );
    }
}
