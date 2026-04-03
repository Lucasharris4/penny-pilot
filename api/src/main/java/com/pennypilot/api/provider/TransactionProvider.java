package com.pennypilot.api.provider;

import java.time.LocalDate;
import java.util.List;

public interface TransactionProvider {

    List<ProviderAccount> fetchAccounts();

    List<ProviderTransaction> fetchTransactions(String accountId, LocalDate since);
}
