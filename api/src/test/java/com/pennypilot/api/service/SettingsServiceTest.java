package com.pennypilot.api.service;

import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.entity.UserProviderCredential;
import com.pennypilot.api.provider.CredentialResolver;
import com.pennypilot.api.provider.SimpleFINProvider;
import com.pennypilot.api.repository.ProviderRepository;
import com.pennypilot.api.repository.UserProviderCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsServiceTest {

    private UserProviderCredentialRepository credentialRepository;
    private CredentialResolver credentialResolver;
    private SimpleFINProvider simpleFINProvider;
    private ProviderRepository providerRepository;
    private SettingsService settingsService;

    private static final Long SIMPLEFIN_PROVIDER_ID = 2L;

    @BeforeEach
    void setUp() {
        credentialRepository = mock(UserProviderCredentialRepository.class);
        credentialResolver = mock(CredentialResolver.class);
        simpleFINProvider = mock(SimpleFINProvider.class);
        providerRepository = mock(ProviderRepository.class);

        Provider simplefinProvider = new Provider();
        simplefinProvider.setId(SIMPLEFIN_PROVIDER_ID);
        simplefinProvider.setName(ProviderType.SIMPLEFIN);
        when(providerRepository.findByName(ProviderType.SIMPLEFIN))
                .thenReturn(Optional.of(simplefinProvider));

        settingsService = new SettingsService(
                credentialRepository, credentialResolver, simpleFINProvider, providerRepository);
    }

    // --- hasSimpleFINCredentials ---

    @Test
    void hasSimpleFINCredentials_true() {
        UserProviderCredential cred = new UserProviderCredential();
        when(credentialRepository.findByUserIdAndProviderId(1L, SIMPLEFIN_PROVIDER_ID))
                .thenReturn(Optional.of(cred));

        assertTrue(settingsService.hasSimpleFINCredentials(1L));
    }

    @Test
    void hasSimpleFINCredentials_false() {
        when(credentialRepository.findByUserIdAndProviderId(1L, SIMPLEFIN_PROVIDER_ID))
                .thenReturn(Optional.empty());

        assertFalse(settingsService.hasSimpleFINCredentials(1L));
    }

    // --- updateSimpleFINToken ---

    @Test
    void updateSimpleFINToken_claimsAndStores() {
        when(simpleFINProvider.claimSetupToken("setup-token-123"))
                .thenReturn("https://bridge.simplefin.org/access-url");

        settingsService.updateSimpleFINToken(1L, "setup-token-123");

        verify(simpleFINProvider).claimSetupToken("setup-token-123");
        verify(credentialResolver).store(1L, SIMPLEFIN_PROVIDER_ID, "https://bridge.simplefin.org/access-url");
    }

    // --- deleteSimpleFINCredentials ---

    @Test
    void deleteSimpleFINCredentials_deletesWhenExists() {
        UserProviderCredential cred = new UserProviderCredential();
        when(credentialRepository.findByUserIdAndProviderId(1L, SIMPLEFIN_PROVIDER_ID))
                .thenReturn(Optional.of(cred));

        settingsService.deleteSimpleFINCredentials(1L);

        verify(credentialRepository).delete(cred);
    }

    @Test
    void deleteSimpleFINCredentials_noOpWhenMissing() {
        when(credentialRepository.findByUserIdAndProviderId(1L, SIMPLEFIN_PROVIDER_ID))
                .thenReturn(Optional.empty());

        settingsService.deleteSimpleFINCredentials(1L);

        verify(credentialRepository, never()).delete(any());
    }
}
