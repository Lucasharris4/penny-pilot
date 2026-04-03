package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
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
    public List<ProviderAccount> fetchAccounts() {
        return accounts;
    }

    @Override
    public List<ProviderTransaction> fetchTransactions(String accountId, LocalDate since) {
        return transactions.stream()
                .filter(t -> t.accountId().equals(accountId))
                .filter(t -> !t.date().isBefore(since))
                .toList();
    }
}
