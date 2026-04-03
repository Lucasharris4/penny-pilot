package com.pennypilot.api.provider;

import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.entity.UserProviderCredential;
import com.pennypilot.api.provider.credentials.ProviderCredentials;
import com.pennypilot.api.provider.credentials.SimpleFINCredentials;
import com.pennypilot.api.repository.UserProviderCredentialRepository;
import com.pennypilot.api.util.CredentialEncryptor;
import org.springframework.stereotype.Component;

@Component
public class CredentialResolver {

    private final UserProviderCredentialRepository credentialRepository;
    private final CredentialEncryptor encryptor;

    public CredentialResolver(UserProviderCredentialRepository credentialRepository,
                              CredentialEncryptor encryptor) {
        this.credentialRepository = credentialRepository;
        this.encryptor = encryptor;
    }

    public ProviderCredentials resolve(Long userId, Long providerId, ProviderType providerType) {
        if (providerType == ProviderType.MOCK) {
            return null;
        }

        UserProviderCredential stored = credentialRepository
                .findByUserIdAndProviderId(userId, providerId)
                .orElseThrow(() -> new CredentialNotFoundException(userId, providerId));

        String decrypted = encryptor.decrypt(stored.getCredential());

        return switch (providerType) {
            case SIMPLEFIN -> new SimpleFINCredentials(decrypted);
            case MOCK -> null;
        };
    }

    public void store(Long userId, Long providerId, String credential) {
        UserProviderCredential entity = credentialRepository
                .findByUserIdAndProviderId(userId, providerId)
                .orElseGet(() -> {
                    UserProviderCredential newEntity = new UserProviderCredential();
                    newEntity.setUserId(userId);
                    newEntity.setProviderId(providerId);
                    return newEntity;
                });

        entity.setCredential(encryptor.encrypt(credential));
        credentialRepository.save(entity);
    }

    public static class CredentialNotFoundException extends RuntimeException {
        public CredentialNotFoundException(Long userId, Long providerId) {
            super("Credentials not found for user " + userId + " and provider " + providerId);
        }
    }
}
