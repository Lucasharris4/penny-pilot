package com.pennypilot.api.dto.provider;

import com.pennypilot.api.entity.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Provider information")
public record ProviderResponse(
        @Schema(description = "Provider ID")
        Long id,

        @Schema(description = "Provider name")
        ProviderType name,

        @Schema(description = "Provider description")
        String description
) {}
