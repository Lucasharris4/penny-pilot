package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.provider.credentials.ProviderCredentials;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TransactionProvider {

    ProviderCredentials resolveCredentialsForLinking(Long userId, Map<String, String> args);

    List<ProviderAccount> fetchAccounts(ProviderCredentials credentials);

    List<ProviderTransaction> fetchTransactions(ProviderCredentials credentials, String accountId, LocalDate since, LocalDate until);
}
