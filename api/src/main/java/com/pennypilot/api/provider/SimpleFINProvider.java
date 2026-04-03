package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.provider.credentials.ProviderCredentials;
import com.pennypilot.api.provider.credentials.SimpleFINCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleFINProvider implements TransactionProvider {

    private static final String CLAIM_URL = "https://bridge.simplefin.org/simplefin/claim";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SimpleFINProvider(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    public String claimSetupToken(String setupToken) {
        try {
            String accessUrl = restClient.post()
                    .uri(CLAIM_URL)
                    .body(setupToken)
                    .retrieve()
                    .body(String.class);

            if (accessUrl == null || accessUrl.isBlank()) {
                throw new ProviderAuthException("SimpleFIN returned empty access URL");
            }

            return accessUrl.trim();
        } catch (RestClientException e) {
            throw new ProviderConnectionException("Failed to claim SimpleFIN setup token", e);
        }
    }

    @Override
    public List<ProviderAccount> fetchAccounts(ProviderCredentials credentials) {
        String accessUrl = extractAccessUrl(credentials);
        try {
            String response = restClient.get()
                    .uri(accessUrl + "/accounts")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode accountsNode = root.path("accounts");

            List<ProviderAccount> accounts = new ArrayList<>();
            for (JsonNode acct : accountsNode) {
                accounts.add(mapAccount(acct));
            }
            return accounts;
        } catch (RestClientException e) {
            throw new ProviderConnectionException("Failed to fetch SimpleFIN accounts", e);
        } catch (Exception e) {
            throw new ProviderConnectionException("Failed to parse SimpleFIN accounts response", e);
        }
    }

    @Override
    public List<ProviderTransaction> fetchTransactions(ProviderCredentials credentials, String accountId, LocalDate since, LocalDate until) {
        String accessUrl = extractAccessUrl(credentials);
        try {
            long startDate = since.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            String uri = accessUrl + "/accounts?account=" + accountId + "&start-date=" + startDate;
            if (until != null) {
                long endDate = until.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
                uri += "&end-date=" + endDate;
            }

            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode accountsNode = root.path("accounts");

            List<ProviderTransaction> transactions = new ArrayList<>();
            for (JsonNode acct : accountsNode) {
                JsonNode txnsNode = acct.path("transactions");
                for (JsonNode txn : txnsNode) {
                    transactions.add(mapTransaction(accountId, txn));
                }
            }
            return transactions;
        } catch (RestClientException e) {
            throw new ProviderConnectionException("Failed to fetch SimpleFIN transactions", e);
        } catch (Exception e) {
            throw new ProviderConnectionException("Failed to parse SimpleFIN transactions response", e);
        }
    }

    private ProviderAccount mapAccount(JsonNode node) {
        String id = node.path("id").asText();
        String name = node.path("name").asText();
        int balanceCents = toCents(node.path("balance").asText("0"));
        return new ProviderAccount(id, name, balanceCents);
    }

    private ProviderTransaction mapTransaction(String accountId, JsonNode node) {
        String id = node.path("id").asText();
        String amountStr = node.path("amount").asText("0");
        int amountCents = toCents(amountStr);
        TransactionType type = amountCents >= 0 ? TransactionType.CREDIT : TransactionType.DEBIT;

        String payee = node.path("payee").asText("");
        String memo = node.path("memo").asText("");
        String description = memo.isBlank() ? node.path("description").asText("") : memo;

        long posted = node.path("posted").asLong();
        LocalDate date = Instant.ofEpochSecond(posted).atZone(ZoneOffset.UTC).toLocalDate();

        return new ProviderTransaction(
                id,
                accountId,
                Math.abs(amountCents),
                type,
                description,
                payee,
                date
        );
    }

    static int toCents(String amountStr) {
        return new BigDecimal(amountStr)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String extractAccessUrl(ProviderCredentials credentials) {
        if (!(credentials instanceof SimpleFINCredentials simpleFIN)) {
            throw new ProviderAuthException("SimpleFIN requires SimpleFINCredentials");
        }
        return simpleFIN.accessUrl();
    }

    public static class ProviderAuthException extends RuntimeException {
        public ProviderAuthException(String message) {
            super(message);
        }
    }

    public static class ProviderConnectionException extends RuntimeException {
        public ProviderConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
