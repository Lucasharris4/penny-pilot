package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.provider.credentials.ProviderCredentials;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class MockProvider implements TransactionProvider {

    private final List<ProviderAccount> accounts;
    private final List<ProviderTransaction> transactions;

    public MockProvider() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try (InputStream is = new ClassPathResource("mock-provider-data.json").getInputStream()) {
            JsonNode root = mapper.readTree(is);
            accounts = mapper.convertValue(root.get("accounts"), new TypeReference<>() {});
            transactions = mapper.convertValue(root.get("transactions"), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load mock provider data", e);
        }
    }

    @Override
    public ProviderCredentials resolveCredentialsForLinking(Long userId, Map<String, String> args) {
        return null;
    }

    @Override
    public List<ProviderAccount> fetchAccounts(ProviderCredentials credentials) {
        return accounts;
    }

    @Override
    public List<ProviderTransaction> fetchTransactions(ProviderCredentials credentials, String accountId, LocalDate since, LocalDate until) {
        return transactions.stream()
                .filter(t -> t.accountId().equals(accountId))
                .filter(t -> !t.date().isBefore(since))
                .filter(t -> until == null || !t.date().isAfter(until))
                .toList();
    }
}
