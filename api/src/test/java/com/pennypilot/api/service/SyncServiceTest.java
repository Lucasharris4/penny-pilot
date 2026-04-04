package com.pennypilot.api.service;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.dto.sync.SyncResponse;
import com.pennypilot.api.entity.*;
import com.pennypilot.api.provider.CredentialResolver;
import com.pennypilot.api.provider.ProviderResolver;
import com.pennypilot.api.provider.TransactionProvider;
import com.pennypilot.api.repository.AccountRepository;
import com.pennypilot.api.repository.CategoryRuleRepository;
import com.pennypilot.api.repository.TransactionRepository;
import com.pennypilot.api.util.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private CategoryRuleRepository categoryRuleRepository;
    private TransactionProvider mockProvider;
    private CredentialResolver credentialResolver;
    private CategoryRuleService categoryRuleService;
    private SyncService syncService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-04-03T12:00:00Z");
    private Provider providerEntity;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        categoryRuleRepository = mock(CategoryRuleRepository.class);
        mockProvider = mock(TransactionProvider.class);
        credentialResolver = mock(CredentialResolver.class);

        // Real CategoryRuleService with mocked repos (per testing philosophy)
        categoryRuleService = new CategoryRuleService(categoryRuleRepository,
                mock(com.pennypilot.api.repository.CategoryRepository.class));

        Map<ProviderType, TransactionProvider> providerMap = Map.of(ProviderType.MOCK, mockProvider);
        ProviderResolver providerResolver = new ProviderResolver(providerMap);
        FixedClock clock = new FixedClock(NOW);

        syncService = new SyncService(accountRepository, transactionRepository, categoryRuleRepository,
                providerResolver, credentialResolver, categoryRuleService, clock);

        providerEntity = new Provider();
        providerEntity.setId(1L);
        providerEntity.setName(ProviderType.MOCK);
    }

    @Test
    void syncAccount_addsNewTransactions() {
        Account account = makeAccount(ACCOUNT_ID, USER_ID, "acct-001", null);
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(credentialResolver.resolve(USER_ID, 1L, ProviderType.MOCK)).thenReturn(null);
        when(mockProvider.fetchAccounts(null)).thenReturn(List.of(
                new ProviderAccount("acct-001", "Checking", 150000)));
        when(mockProvider.fetchTransactions(eq(null), eq("acct-001"), any(), any())).thenReturn(List.of(
                new ProviderTransaction("txn-001", "acct-001", 4599, TransactionType.DEBIT,
                        "Coffee purchase", "STARBUCKS", LocalDate.of(2026, 3, 15)),
                new ProviderTransaction("txn-002", "acct-001", 250000, TransactionType.CREDIT,
                        "Payroll", "EMPLOYER INC", LocalDate.of(2026, 3, 16))
        ));
        when(transactionRepository.findByAccountIdAndExternalIdIn(eq(ACCOUNT_ID), anyList()))
                .thenReturn(List.of());
        when(categoryRuleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncResponse response = syncService.syncAccount(USER_ID, ACCOUNT_ID);

        assertEquals(2, response.transactionsAdded());
        assertEquals(0, response.transactionsUpdated());
        assertEquals(0, response.transactionsSkipped());
        assertEquals(150000, response.accountBalanceCents());
        assertEquals(NOW, response.syncedAt());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void syncAccount_skipsDuplicates() {
        Account account = makeAccount(ACCOUNT_ID, USER_ID, "acct-001", Instant.parse("2026-03-01T00:00:00Z"));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(credentialResolver.resolve(USER_ID, 1L, ProviderType.MOCK)).thenReturn(null);
        when(mockProvider.fetchAccounts(null)).thenReturn(List.of(
                new ProviderAccount("acct-001", "Checking", 150000)));
        when(mockProvider.fetchTransactions(eq(null), eq("acct-001"), any(), any())).thenReturn(List.of(
                new ProviderTransaction("txn-001", "acct-001", 4599, TransactionType.DEBIT,
                        "Coffee purchase", "STARBUCKS", LocalDate.of(2026, 3, 15))
        ));

        Transaction existing = new Transaction();
        existing.setExternalId("txn-001");
        existing.setAmountCents(4599);
        existing.setTransactionType(TransactionType.DEBIT);
        existing.setDescription("Coffee purchase");
        existing.setMerchantName("STARBUCKS");
        existing.setDate("2026-03-15");

        when(transactionRepository.findByAccountIdAndExternalIdIn(eq(ACCOUNT_ID), anyList()))
                .thenReturn(List.of(existing));
        when(categoryRuleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncResponse response = syncService.syncAccount(USER_ID, ACCOUNT_ID);

        assertEquals(0, response.transactionsAdded());
        assertEquals(0, response.transactionsUpdated());
        assertEquals(1, response.transactionsSkipped());
    }

    @Test
    void syncAccount_updatesChangedTransactions() {
        Account account = makeAccount(ACCOUNT_ID, USER_ID, "acct-001", Instant.parse("2026-03-01T00:00:00Z"));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(credentialResolver.resolve(USER_ID, 1L, ProviderType.MOCK)).thenReturn(null);
        when(mockProvider.fetchAccounts(null)).thenReturn(List.of(
                new ProviderAccount("acct-001", "Checking", 150000)));
        when(mockProvider.fetchTransactions(eq(null), eq("acct-001"), any(), any())).thenReturn(List.of(
                new ProviderTransaction("txn-001", "acct-001", 5000, TransactionType.DEBIT,
                        "Updated description", "STARBUCKS", LocalDate.of(2026, 3, 15))
        ));

        Transaction existing = new Transaction();
        existing.setExternalId("txn-001");
        existing.setAmountCents(4599);
        existing.setTransactionType(TransactionType.DEBIT);
        existing.setDescription("Coffee purchase");
        existing.setMerchantName("STARBUCKS");
        existing.setDate("2026-03-15");

        when(transactionRepository.findByAccountIdAndExternalIdIn(eq(ACCOUNT_ID), anyList()))
                .thenReturn(List.of(existing));
        when(categoryRuleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncResponse response = syncService.syncAccount(USER_ID, ACCOUNT_ID);

        assertEquals(0, response.transactionsAdded());
        assertEquals(1, response.transactionsUpdated());
        assertEquals(0, response.transactionsSkipped());
        assertEquals(5000, existing.getAmountCents());
        assertEquals("Updated description", existing.getDescription());
    }

    @Test
    void syncAccount_autoCategorizes() {
        Account account = makeAccount(ACCOUNT_ID, USER_ID, "acct-001", null);
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(credentialResolver.resolve(USER_ID, 1L, ProviderType.MOCK)).thenReturn(null);
        when(mockProvider.fetchAccounts(null)).thenReturn(List.of(
                new ProviderAccount("acct-001", "Checking", 150000)));
        when(mockProvider.fetchTransactions(eq(null), eq("acct-001"), any(), any())).thenReturn(List.of(
                new ProviderTransaction("txn-001", "acct-001", 4599, TransactionType.DEBIT,
                        "Coffee", "STARBUCKS #1234", LocalDate.of(2026, 3, 15))
        ));
        when(transactionRepository.findByAccountIdAndExternalIdIn(eq(ACCOUNT_ID), anyList()))
                .thenReturn(List.of());

        CategoryRule rule = new CategoryRule();
        rule.setMatchPattern("STARBUCKS*");
        rule.setCategoryId(5L);
        rule.setPriority(10);
        when(categoryRuleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of(rule));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncResponse response = syncService.syncAccount(USER_ID, ACCOUNT_ID);

        assertEquals(1, response.transactionsAdded());
        verify(transactionRepository).save(argThat(txn -> txn.getCategoryId() != null && txn.getCategoryId() == 5L));
    }

    @Test
    void syncAccount_updatesBalance() {
        Account account = makeAccount(ACCOUNT_ID, USER_ID, "acct-001", null);
        account.setBalanceCents(100000);
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(credentialResolver.resolve(USER_ID, 1L, ProviderType.MOCK)).thenReturn(null);
        when(mockProvider.fetchAccounts(null)).thenReturn(List.of(
                new ProviderAccount("acct-001", "Checking", 200000)));
        when(mockProvider.fetchTransactions(eq(null), eq("acct-001"), any(), any())).thenReturn(List.of());
        when(transactionRepository.findByAccountIdAndExternalIdIn(anyLong(), anyList())).thenReturn(List.of());
        when(categoryRuleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncResponse response = syncService.syncAccount(USER_ID, ACCOUNT_ID);

        assertEquals(200000, account.getBalanceCents());
        assertEquals(200000, response.accountBalanceCents());
    }

    @Test
    void syncAccount_updatesLastSyncedAt() {
        Account account = makeAccount(ACCOUNT_ID, USER_ID, "acct-001", null);
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(credentialResolver.resolve(USER_ID, 1L, ProviderType.MOCK)).thenReturn(null);
        when(mockProvider.fetchAccounts(null)).thenReturn(List.of(
                new ProviderAccount("acct-001", "Checking", 150000)));
        when(mockProvider.fetchTransactions(eq(null), eq("acct-001"), any(), any())).thenReturn(List.of());
        when(transactionRepository.findByAccountIdAndExternalIdIn(anyLong(), anyList())).thenReturn(List.of());
        when(categoryRuleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        syncService.syncAccount(USER_ID, ACCOUNT_ID);

        assertEquals(NOW, account.getLastSyncedAt());
    }

    @Test
    void syncAccount_notFound_throws() {
        when(accountRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(SyncService.AccountNotFoundException.class,
                () -> syncService.syncAccount(USER_ID, 99L));
    }

    // --- helpers ---

    private Account makeAccount(Long id, Long userId, String providerAccountId, Instant lastSyncedAt) {
        Account a = new Account();
        a.setId(id);
        a.setUserId(userId);
        a.setProvider(providerEntity);
        a.setProviderAccountId(providerAccountId);
        a.setAccountName("Test Account");
        a.setBalanceCents(0);
        a.setLastSyncedAt(lastSyncedAt);
        return a;
    }
}
