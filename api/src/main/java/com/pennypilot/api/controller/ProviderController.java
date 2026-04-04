package com.pennypilot.api.controller;

import com.pennypilot.api.dto.provider.ProviderResponse;
import com.pennypilot.api.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Providers", description = "Provider listing endpoint")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    @Operation(summary = "List available providers")
    @ApiResponse(responseCode = "200", description = "Providers retrieved")
    public ResponseEntity<List<ProviderResponse>> listProviders() {
        return ResponseEntity.ok(providerService.listAvailableProviders());
    }
}
