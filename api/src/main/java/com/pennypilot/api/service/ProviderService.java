package com.pennypilot.api.service;

import com.pennypilot.api.config.ProviderProperties;
import com.pennypilot.api.dto.provider.ProviderResponse;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderProperties providerProperties;

    public ProviderService(ProviderRepository providerRepository, ProviderProperties providerProperties) {
        this.providerRepository = providerRepository;
        this.providerProperties = providerProperties;
    }

    public List<ProviderResponse> listAvailableProviders() {
        return providerRepository.findAll().stream()
                .filter(p -> providerProperties.mockEnabled() || p.getName() != ProviderType.MOCK)
                .map(p -> new ProviderResponse(p.getId(), p.getName(), p.getDescription()))
                .toList();
    }
}
