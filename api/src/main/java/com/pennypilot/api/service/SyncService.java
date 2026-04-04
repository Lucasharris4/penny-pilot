package com.pennypilot.api.service;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.dto.sync.SyncResponse;
import com.pennypilot.api.entity.Account;
import com.pennypilot.api.entity.CategoryRule;
import com.pennypilot.api.entity.Transaction;
import com.pennypilot.api.provider.CredentialResolver;
import com.pennypilot.api.provider.ProviderResolver;
import com.pennypilot.api.provider.TransactionProvider;
import com.pennypilot.api.provider.credentials.ProviderCredentials;
import com.pennypilot.api.repository.AccountRepository;
import com.pennypilot.api.repository.CategoryRuleRepository;
import com.pennypilot.api.repository.TransactionRepository;
import com.pennypilot.api.util.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final ProviderResolver providerResolver;
    private final CredentialResolver credentialResolver;
    private final CategoryRuleService categoryRuleService;
    private final Clock clock;

    public SyncService(AccountRepository accountRepository,
                       TransactionRepository transactionRepository,
                       CategoryRuleRepository categoryRuleRepository,
                       ProviderResolver providerResolver,
                       CredentialResolver credentialResolver,
                       CategoryRuleService categoryRuleService,
                       Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.providerResolver = providerResolver;
        this.credentialResolver = credentialResolver;
        this.categoryRuleService = categoryRuleService;
        this.clock = clock;
    }

    @Transactional
    public SyncResponse syncAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        TransactionProvider provider = providerResolver.resolve(account.getProvider().getName());
        ProviderCredentials credentials = credentialResolver.resolve(
                userId, account.getProvider().getId(), account.getProvider().getName());

        // Update balance
        updateAccountBalance(account, provider, credentials);

        // Fetch transactions
        LocalDate since = account.getLastSyncedAt() != null
                ? account.getLastSyncedAt().atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.EPOCH;
        List<ProviderTransaction> providerTxns = provider.fetchTransactions(
                credentials, account.getProviderAccountId(), since, LocalDate.now());

        // Dedup
        List<String> externalIds = providerTxns.stream()
                .map(ProviderTransaction::transactionId)
                .toList();

        Map<String, Transaction> existingByExternalId = externalIds.isEmpty()
                ? Map.of()
                : transactionRepository.findByAccountIdAndExternalIdIn(accountId, externalIds).stream()
                        .collect(Collectors.toMap(Transaction::getExternalId, Function.identity()));

        // Load categorization rules
        List<CategoryRule> rules = categoryRuleRepository.findByUserIdOrderByPriorityDesc(userId);

        int added = 0;
        int updated = 0;
        int skipped = 0;

        for (ProviderTransaction pt : providerTxns) {
            Transaction existing = existingByExternalId.get(pt.transactionId());

            if (existing != null) {
                if (hasChanged(existing, pt)) {
                    applyProviderData(existing, pt);
                    transactionRepository.save(existing);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                Transaction txn = new Transaction();
                txn.setUserId(userId);
                txn.setAccountId(accountId);
                txn.setExternalId(pt.transactionId());
                applyProviderData(txn, pt);

                // Auto-categorize
                if (pt.merchantName() != null && !pt.merchantName().isBlank()) {
                    Optional<Long> categoryId = categoryRuleService.findMatchingCategoryId(rules, pt.merchantName());
                    categoryId.ifPresent(txn::setCategoryId);
                }

                transactionRepository.save(txn);
                added++;
            }
        }

        // Update lastSyncedAt
        account.setLastSyncedAt(clock.now());
        accountRepository.save(account);

        return new SyncResponse(added, updated, skipped, account.getBalanceCents(), clock.now());
    }

    private void updateAccountBalance(Account account, TransactionProvider provider, ProviderCredentials credentials) {
        List<ProviderAccount> providerAccounts = provider.fetchAccounts(credentials);
        providerAccounts.stream()
                .filter(pa -> pa.accountId().equals(account.getProviderAccountId()))
                .findFirst()
                .ifPresent(pa -> account.setBalanceCents(pa.balanceCents()));
    }

    private boolean hasChanged(Transaction existing, ProviderTransaction pt) {
        return !existing.getAmountCents().equals(pt.amountCents())
                || existing.getTransactionType() != pt.transactionType()
                || !safeEquals(existing.getDescription(), pt.description())
                || !safeEquals(existing.getMerchantName(), pt.merchantName())
                || !safeEquals(existing.getDate(), pt.date().toString());
    }

    private void applyProviderData(Transaction txn, ProviderTransaction pt) {
        txn.setAmountCents(pt.amountCents());
        txn.setTransactionType(pt.transactionType());
        txn.setDescription(pt.description());
        txn.setMerchantName(pt.merchantName());
        txn.setDate(pt.date().toString());
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(Long id) {
            super("Account not found: " + id);
        }
    }
}
