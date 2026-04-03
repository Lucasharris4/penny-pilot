package com.pennypilot.api.dto.transaction;

import com.pennypilot.api.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update transaction request")
public record UpdateTransactionRequest(
        @Schema(description = "Category ID (null to uncategorize)")
        Long categoryId,

        @Schema(description = "Amount in cents (unsigned, always positive)", example = "4500")
        @NotNull(message = "Amount is required")
        Integer amountCents,

        @Schema(description = "Transaction type: CREDIT or DEBIT")
        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @Schema(description = "Transaction description", example = "WHOLE FOODS #1234")
        @NotBlank(message = "Description is required")
        String description,

        @Schema(description = "Normalized merchant name", example = "Whole Foods")
        String merchantName,

        @Schema(description = "ISO 8601 date", example = "2026-03-15")
        @NotBlank(message = "Date is required")
        String date
) {}
