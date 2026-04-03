package com.pennypilot.api.config;

import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.repository.ProviderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProviderSeeder implements ApplicationRunner {

    private final ProviderRepository providerRepository;

    public ProviderSeeder(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing(ProviderType.MOCK, "Sandbox provider with sample data");
        seedIfMissing(ProviderType.SIMPLEFIN, "SimpleFIN Bridge");
    }

    private void seedIfMissing(ProviderType name, String description) {
        if (providerRepository.findByName(name).isEmpty()) {
            Provider provider = new Provider();
            provider.setName(name);
            provider.setDescription(description);
            providerRepository.save(provider);
        }
    }
}
