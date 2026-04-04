package com.pennypilot.api.service;

import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.provider.CredentialResolver;
import com.pennypilot.api.provider.SimpleFINProvider;
import com.pennypilot.api.repository.ProviderRepository;
import com.pennypilot.api.repository.UserProviderCredentialRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final UserProviderCredentialRepository credentialRepository;
    private final CredentialResolver credentialResolver;
    private final SimpleFINProvider simpleFINProvider;
    private final ProviderRepository providerRepository;

    public SettingsService(UserProviderCredentialRepository credentialRepository,
                           CredentialResolver credentialResolver,
                           SimpleFINProvider simpleFINProvider,
                           ProviderRepository providerRepository) {
        this.credentialRepository = credentialRepository;
        this.credentialResolver = credentialResolver;
        this.simpleFINProvider = simpleFINProvider;
        this.providerRepository = providerRepository;
    }

    public boolean hasSimpleFINCredentials(Long userId) {
        Long providerId = getSimpleFINProviderId();
        return credentialRepository.findByUserIdAndProviderId(userId, providerId).isPresent();
    }

    public void updateSimpleFINToken(Long userId, String setupToken) {
        String accessUrl = simpleFINProvider.claimSetupToken(setupToken);
        Long providerId = getSimpleFINProviderId();
        credentialResolver.store(userId, providerId, accessUrl);
    }

    public void deleteSimpleFINCredentials(Long userId) {
        Long providerId = getSimpleFINProviderId();
        credentialRepository.findByUserIdAndProviderId(userId, providerId)
                .ifPresent(credentialRepository::delete);
    }

    private Long getSimpleFINProviderId() {
        Provider provider = providerRepository.findByName(ProviderType.SIMPLEFIN)
                .orElseThrow(() -> new IllegalStateException("SimpleFIN provider not found in database"));
        return provider.getId();
    }
}
