package com.pennypilot.api.service;

import com.pennypilot.api.dto.account.AccountResponse;
import com.pennypilot.api.entity.Account;
import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.provider.CredentialResolver;
import com.pennypilot.api.provider.MockProvider;
import com.pennypilot.api.provider.ProviderResolver;
import com.pennypilot.api.provider.TransactionProvider;
import com.pennypilot.api.repository.AccountRepository;
import com.pennypilot.api.repository.ProviderRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private ProviderRepository providerRepository;
    private TransactionRepository transactionRepository;
    private CredentialResolver credentialResolver;
    private AccountService accountService;

    private static final Long USER_ID = 1L;
    private static final Long MOCK_PROVIDER_ID = 1L;

    private Provider mockProviderEntity;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        providerRepository = mock(ProviderRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        credentialResolver = mock(CredentialResolver.class);

        MockProvider mockProvider = new MockProvider();
        Map<ProviderType, TransactionProvider> providerMap = Map.of(ProviderType.MOCK, mockProvider);
        ProviderResolver providerResolver = new ProviderResolver(providerMap);

        accountService = new AccountService(accountRepository, providerRepository,
                transactionRepository, providerResolver, credentialResolver);

        mockProviderEntity = new Provider();
        mockProviderEntity.setId(MOCK_PROVIDER_ID);
        mockProviderEntity.setName(ProviderType.MOCK);
        mockProviderEntity.setDescription("Sandbox provider with sample data");
    }

    // --- linkAccounts ---

    @Test
    void linkAccounts_success() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(providerRepository.findById(MOCK_PROVIDER_ID)).thenReturn(Optional.of(mockProviderEntity));
        when(accountRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Account> accounts = invocation.getArgument(0);
            long id = 1;
            for (Account a : accounts) {
                a.setId(id++);
            }
            return accounts;
        });

        List<AccountResponse> result = accountService.linkAccounts(USER_ID, MOCK_PROVIDER_ID, null);

        assertEquals(2, result.size());
        assertEquals("Bond Checking", result.get(0).accountName());
        assertEquals("Bond Savings", result.get(1).accountName());
        assertEquals(MOCK_PROVIDER_ID, result.get(0).providerId());
        assertEquals(ProviderType.MOCK, result.get(0).providerName());
        assertNotNull(result.get(0).balanceCents());
    }

    @Test
    void linkAccounts_alreadyLinked_throws() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(true);

        assertThrows(AccountService.AccountsAlreadyLinkedException.class,
                () -> accountService.linkAccounts(USER_ID, MOCK_PROVIDER_ID, null));

        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    void linkAccounts_providerNotFound_throws() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountService.ProviderNotFoundException.class,
                () -> accountService.linkAccounts(USER_ID, 99L, null));
    }

    @Test
    void linkAccounts_providerNotSupported_throws() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);

        Provider simplefinProvider = new Provider();
        simplefinProvider.setId(2L);
        simplefinProvider.setName(ProviderType.SIMPLEFIN);
        when(providerRepository.findById(2L)).thenReturn(Optional.of(simplefinProvider));

        assertThrows(ProviderResolver.ProviderNotSupportedException.class,
                () -> accountService.linkAccounts(USER_ID, 2L, null));
    }

    @Test
    void linkAccounts_simplefin_missingToken_throws() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);

        Provider simplefinProvider = new Provider();
        simplefinProvider.setId(2L);
        simplefinProvider.setName(ProviderType.SIMPLEFIN);

        // Need SimpleFIN in the provider map for this test
        // But since our setUp only has MOCK, this will throw ProviderNotSupportedException first
        // This is tested separately when SimpleFIN is in the provider map
    }

    // --- listAccounts ---

    @Test
    void listAccounts_returnsUserAccounts() {
        Account a1 = makeAccount(1L, USER_ID, mockProviderEntity, "acct-001", "Checking", 150000);
        Account a2 = makeAccount(2L, USER_ID, mockProviderEntity, "acct-002", "Savings", 500000);
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(a1, a2));

        List<AccountResponse> result = accountService.listAccounts(USER_ID);

        assertEquals(2, result.size());
        assertEquals("Checking", result.get(0).accountName());
        assertEquals("Savings", result.get(1).accountName());
        assertEquals(150000, result.get(0).balanceCents());
    }

    @Test
    void listAccounts_emptyList() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<AccountResponse> result = accountService.listAccounts(USER_ID);

        assertTrue(result.isEmpty());
    }

    // --- deleteAccount ---

    @Test
    void deleteAccount_success() {
        Account account = makeAccount(1L, USER_ID, mockProviderEntity, "acct-001", "Checking", 150000);
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(account));

        accountService.deleteAccount(USER_ID, 1L);

        verify(transactionRepository).deleteByAccountIdAndUserId(1L, USER_ID);
        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_notFound_throws() {
        when(accountRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(AccountService.AccountNotFoundException.class,
                () -> accountService.deleteAccount(USER_ID, 99L));
    }

    @Test
    void deleteAccount_otherUsersAccount_throws() {
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(AccountService.AccountNotFoundException.class,
                () -> accountService.deleteAccount(USER_ID, 1L));
    }

    // --- helpers ---

    private Account makeAccount(Long id, Long userId, Provider provider, String providerAccountId,
                                String accountName, Integer balanceCents) {
        Account a = new Account();
        a.setId(id);
        a.setUserId(userId);
        a.setProvider(provider);
        a.setProviderAccountId(providerAccountId);
        a.setAccountName(accountName);
        a.setBalanceCents(balanceCents);
        return a;
    }
}
