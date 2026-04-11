package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.provider.credentials.ProviderCredentials;
import com.pennypilot.api.provider.credentials.SimpleFINCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class SimpleFINProvider implements TransactionProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CredentialResolver credentialResolver;

    public SimpleFINProvider(RestClient.Builder restClientBuilder, CredentialResolver credentialResolver) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.credentialResolver = credentialResolver;
    }

    @Override
    public ProviderCredentials resolveCredentialsForLinking(Long userId, Map<String, String> args) {
        String setupToken = args.get("setupToken");
        if (setupToken == null || setupToken.isBlank()) {
            throw new SetupTokenRequiredException();
        }
        String accessUrl = claimSetupToken(setupToken);
        String providerId = args.get("providerId");
        credentialResolver.store(userId, Long.parseLong(providerId), accessUrl);
        return new SimpleFINCredentials(accessUrl);
    }

    public String claimSetupToken(String setupToken) {
        String claimUrl;
        try {
            claimUrl = new String(Base64.getDecoder().decode(setupToken), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ProviderAuthException("Invalid SimpleFIN setup token");
        }

        try {
            String accessUrl = restClient.post()
                    .uri(claimUrl)
                    .retrieve()
                    .body(String.class);

            if (accessUrl == null || accessUrl.isBlank()) {
                throw new ProviderAuthException("SimpleFIN returned empty access URL");
            }

            return accessUrl.trim();
        } catch (HttpClientErrorException.Forbidden e) {
            throw new ProviderAuthException("SimpleFIN setup token is invalid or already used");
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
        String id = node.path("id").asText(null);
        if (id == null) {
            throw new ProviderConnectionException("SimpleFIN account missing 'id' field",
                    new IllegalStateException("Missing required field"));
        }
        String name = node.path("name").asText("Unknown Account");
        int balanceCents = toCents(node.path("balance").asText("0"));
        return new ProviderAccount(id, name, balanceCents);
    }

    private ProviderTransaction mapTransaction(String accountId, JsonNode node) {
        String id = node.path("id").asText(null);
        if (id == null) {
            throw new ProviderConnectionException("SimpleFIN transaction missing 'id' field",
                    new IllegalStateException("Missing required field"));
        }
        String amountStr = node.path("amount").asText("0");
        int amountCents = toCents(amountStr);
        TransactionType type = amountCents >= 0 ? TransactionType.CREDIT : TransactionType.DEBIT;

        String payee = node.path("payee").asText("");
        String memo = node.path("memo").asText("");
        String description = memo.isBlank() ? node.path("description").asText("") : memo;

        long posted = node.path("posted").asLong(0);
        LocalDate date = posted > 0
                ? Instant.ofEpochSecond(posted).atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.EPOCH;

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

    public static class SetupTokenRequiredException extends RuntimeException {
        public SetupTokenRequiredException() {
            super("Setup token is required for SimpleFIN provider");
        }
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
