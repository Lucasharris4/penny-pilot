package com.pennypilot.api.provider;

import com.pennypilot.api.entity.ProviderType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProviderResolver {

    private final Map<ProviderType, TransactionProvider> providerMap;

    public ProviderResolver(Map<ProviderType, TransactionProvider> providerMap) {
        this.providerMap = providerMap;
    }

    public TransactionProvider resolve(ProviderType providerType) {
        TransactionProvider provider = providerMap.get(providerType);
        if (provider == null) {
            throw new ProviderNotSupportedException(providerType);
        }
        return provider;
    }

    public static class ProviderNotSupportedException extends RuntimeException {
        public ProviderNotSupportedException(ProviderType type) {
            super("Provider not supported: " + type);
        }
    }
}
