package com.pennypilot.api.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to link accounts from a provider")
public record LinkAccountsRequest(
        @NotNull
        @Schema(description = "ID of the provider to link accounts from")
        Long providerId
) {}
