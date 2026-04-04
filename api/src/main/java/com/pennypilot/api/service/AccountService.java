package com.pennypilot.api.service;

import com.pennypilot.api.dto.account.AccountResponse;
import com.pennypilot.api.dto.account.LinkAccountsRequest;
import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.entity.Account;
import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.provider.ProviderResolver;
import com.pennypilot.api.provider.TransactionProvider;
import com.pennypilot.api.provider.credentials.ProviderCredentials;
import com.pennypilot.api.repository.AccountRepository;
import com.pennypilot.api.repository.ProviderRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public List<AccountResponse> linkAccounts(Long userId, LinkAccountsRequest request) {
        if (accountRepository.existsByUserId(userId)) {
            throw new AccountsAlreadyLinkedException();
        }

        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ProviderNotFoundException(request.providerId()));

        TransactionProvider transactionProvider = providerResolver.resolve(provider.getName());

        Map<String, String> args = new HashMap<>();
        args.put("providerId", provider.getId().toString());
        if (request.setupToken() != null) {
            args.put("setupToken", request.setupToken());
        }
        ProviderCredentials credentials = transactionProvider.resolveCredentialsForLinking(userId, args);

        List<ProviderAccount> providerAccounts = transactionProvider.fetchAccounts(credentials);

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
