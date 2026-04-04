package com.pennypilot.api.service;

import com.pennypilot.api.config.ProviderProperties;
import com.pennypilot.api.dto.provider.ProviderResponse;
import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderServiceTest {

    private ProviderRepository providerRepository;
    private ProviderProperties providerProperties;
    private ProviderService providerService;

    @BeforeEach
    void setUp() {
        providerRepository = mock(ProviderRepository.class);
        providerProperties = mock(ProviderProperties.class);
        providerService = new ProviderService(providerRepository, providerProperties);
    }

    private Provider makeProvider(Long id, ProviderType name, String description) {
        Provider p = new Provider();
        p.setId(id);
        p.setName(name);
        p.setDescription(description);
        return p;
    }

    @Test
    void listAvailableProviders_mockEnabled_returnsAll() {
        when(providerProperties.mockEnabled()).thenReturn(true);
        when(providerRepository.findAll()).thenReturn(List.of(
                makeProvider(1L, ProviderType.MOCK, "Sandbox provider with sample data"),
                makeProvider(2L, ProviderType.SIMPLEFIN, "SimpleFIN Bridge")
        ));

        List<ProviderResponse> result = providerService.listAvailableProviders();

        assertEquals(2, result.size());
        assertEquals(ProviderType.MOCK, result.get(0).name());
        assertEquals(ProviderType.SIMPLEFIN, result.get(1).name());
    }

    @Test
    void listAvailableProviders_mockDisabled_filtersMock() {
        when(providerProperties.mockEnabled()).thenReturn(false);
        when(providerRepository.findAll()).thenReturn(List.of(
                makeProvider(1L, ProviderType.MOCK, "Sandbox provider with sample data"),
                makeProvider(2L, ProviderType.SIMPLEFIN, "SimpleFIN Bridge")
        ));

        List<ProviderResponse> result = providerService.listAvailableProviders();

        assertEquals(1, result.size());
        assertEquals(ProviderType.SIMPLEFIN, result.get(0).name());
    }
}
