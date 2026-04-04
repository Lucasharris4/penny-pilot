package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockProviderTest {

    private MockProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockProvider();
    }

    @Test
    void fetchAccounts_returnsTwoAccounts() {
        List<ProviderAccount> accounts = provider.fetchAccounts(null);

        assertEquals(2, accounts.size());
    }

    @Test
    void fetchAccounts_containsCheckingAndSavings() {
        List<ProviderAccount> accounts = provider.fetchAccounts(null);
        List<String> names = accounts.stream().map(ProviderAccount::accountName).toList();

        assertTrue(names.contains("Bond Checking"));
        assertTrue(names.contains("Bond Savings"));
    }

    @Test
    void fetchTransactions_filtersByAccountId() {
        List<ProviderTransaction> transactions = provider.fetchTransactions(
                null, "mock-checking-007", LocalDate.of(2026, 1, 1), null);

        assertTrue(transactions.size() > 0);
        assertTrue(transactions.stream().allMatch(t -> t.accountId().equals("mock-checking-007")));
    }

    @Test
    void fetchTransactions_filtersBySinceDate() {
        List<ProviderTransaction> all = provider.fetchTransactions(
                null, "mock-checking-007", LocalDate.of(2026, 1, 1), null);
        List<ProviderTransaction> recent = provider.fetchTransactions(
                null, "mock-checking-007", LocalDate.of(2026, 3, 20), null);

        assertTrue(recent.size() < all.size());
        assertTrue(recent.stream().allMatch(t -> !t.date().isBefore(LocalDate.of(2026, 3, 20))));
    }

    @Test
    void fetchTransactions_filtersByUntilDate() {
        List<ProviderTransaction> all = provider.fetchTransactions(
                null, "mock-checking-007", LocalDate.of(2026, 1, 1), null);
        List<ProviderTransaction> limited = provider.fetchTransactions(
                null, "mock-checking-007", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1));

        assertTrue(limited.size() < all.size());
        assertTrue(limited.stream().allMatch(t -> !t.date().isAfter(LocalDate.of(2026, 3, 1))));
    }

    @Test
    void fetchTransactions_returnsEmptyForUnknownAccount() {
        List<ProviderTransaction> transactions = provider.fetchTransactions(
                null, "nonexistent", LocalDate.of(2026, 1, 1), null);

        assertTrue(transactions.isEmpty());
    }

    @Test
    void fetchTransactions_containsCreditsAndDebits() {
        List<ProviderTransaction> transactions = provider.fetchTransactions(
                null, "mock-checking-007", LocalDate.of(2026, 1, 1), null);

        assertTrue(transactions.stream().anyMatch(t -> t.transactionType() == TransactionType.CREDIT));
        assertTrue(transactions.stream().anyMatch(t -> t.transactionType() == TransactionType.DEBIT));
    }
}
