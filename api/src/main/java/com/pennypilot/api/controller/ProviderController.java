package com.pennypilot.api.controller;

import com.pennypilot.api.config.ProviderProperties;
import com.pennypilot.api.dto.provider.ProviderResponse;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.repository.ProviderRepository;
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

    private final ProviderRepository providerRepository;
    private final ProviderProperties providerProperties;

    public ProviderController(ProviderRepository providerRepository, ProviderProperties providerProperties) {
        this.providerRepository = providerRepository;
        this.providerProperties = providerProperties;
    }

    @GetMapping
    @Operation(summary = "List available providers")
    @ApiResponse(responseCode = "200", description = "Providers retrieved")
    public ResponseEntity<List<ProviderResponse>> listProviders() {
        List<ProviderResponse> providers = providerRepository.findAll().stream()
                .filter(p -> providerProperties.mockEnabled() || p.getName() != ProviderType.MOCK)
                .map(p -> new ProviderResponse(p.getId(), p.getName(), p.getDescription()))
                .toList();
        return ResponseEntity.ok(providers);
    }
}
