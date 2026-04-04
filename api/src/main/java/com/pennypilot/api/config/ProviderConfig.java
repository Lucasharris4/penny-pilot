package com.pennypilot.api.config;

import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.provider.MockProvider;
import com.pennypilot.api.provider.SimpleFINProvider;
import com.pennypilot.api.provider.TransactionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ProviderConfig {

    @Bean
    public Map<ProviderType, TransactionProvider> providerMap(MockProvider mockProvider,
                                                              SimpleFINProvider simpleFINProvider) {
        return Map.of(
                ProviderType.MOCK, mockProvider,
                ProviderType.SIMPLEFIN, simpleFINProvider
        );
    }
}
