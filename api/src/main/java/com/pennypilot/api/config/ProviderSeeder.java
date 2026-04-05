package com.pennypilot.api.config;

import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.repository.ProviderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class ProviderSeeder implements ApplicationRunner {

    private final ProviderRepository providerRepository;

    public ProviderSeeder(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (providerRepository.findByName(ProviderType.MOCK).isEmpty()) {
            Provider provider = new Provider();
            provider.setName(ProviderType.MOCK);
            provider.setDescription("Sandbox provider with sample data");
            providerRepository.save(provider);
        }
    }
}
