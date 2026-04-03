package com.pennypilot.api.service;

import com.pennypilot.api.dto.account.AccountResponse;
import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.entity.Account;
import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.provider.ProviderResolver;
import com.pennypilot.api.provider.TransactionProvider;
import com.pennypilot.api.repository.AccountRepository;
import com.pennypilot.api.repository.ProviderRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProviderRepository providerRepository;
    private final TransactionRepository transactionRepository;
    private final ProviderResolver providerResolver;

    public AccountService(AccountRepository accountRepository,
                          ProviderRepository providerRepository,
                          TransactionRepository transactionRepository,
                          ProviderResolver providerResolver) {
        this.accountRepository = accountRepository;
        this.providerRepository = providerRepository;
        this.transactionRepository = transactionRepository;
        this.providerResolver = providerResolver;
    }

    public List<AccountResponse> linkAccounts(Long userId, Long providerId) {
        if (accountRepository.existsByUserId(userId)) {
            throw new AccountsAlreadyLinkedException();
        }

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));

        TransactionProvider transactionProvider = providerResolver.resolve(provider.getName());
        List<ProviderAccount> providerAccounts = transactionProvider.fetchAccounts();

        List<Account> accounts = providerAccounts.stream()
                .map(pa -> {
                    Account account = new Account();
                    account.setUserId(userId);
                    account.setProvider(provider);
                    account.setProviderAccountId(pa.accountId());
                    account.setAccountName(pa.accountName());
                    account.setBalanceCents(pa.balanceCents());
                    return account;
                })
                .toList();

        List<Account> saved = accountRepository.saveAll(accounts);
        return saved.stream()
                .map(AccountResponse::from)
                .toList();
    }

    public List<AccountResponse> listAccounts(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        transactionRepository.deleteByAccountIdAndUserId(accountId, userId);
        accountRepository.delete(account);
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(Long id) {
            super("Account not found: " + id);
        }
    }

    public static class AccountsAlreadyLinkedException extends RuntimeException {
        public AccountsAlreadyLinkedException() {
            super("Accounts are already linked. Delete existing accounts before linking new ones.");
        }
    }

    public static class ProviderNotFoundException extends RuntimeException {
        public ProviderNotFoundException(Long id) {
            super("Provider not found: " + id);
        }
    }
}
