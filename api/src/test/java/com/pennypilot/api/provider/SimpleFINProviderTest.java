package com.pennypilot.api.provider;

import com.pennypilot.api.dto.provider.ProviderAccount;
import com.pennypilot.api.dto.provider.ProviderTransaction;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.provider.credentials.SimpleFINCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SimpleFINProviderTest {

    private SimpleFINProvider provider;
    private MockRestServiceServer mockServer;

    private static final String ACCESS_URL = "https://user:pass@bridge.simplefin.org/simplefin";
    private static final SimpleFINCredentials CREDENTIALS = new SimpleFINCredentials(ACCESS_URL);

    private static final String ACCOUNTS_RESPONSE = """
            {
              "accounts": [
                {
                  "id": "acct-123",
                  "name": "My Checking",
                  "balance": "1234.56",
                  "currency": "USD",
                  "transactions": []
                },
                {
                  "id": "acct-456",
                  "name": "My Savings",
                  "balance": "-50.00",
                  "currency": "USD",
                  "transactions": []
                }
              ]
            }
            """;

    private static final String TRANSACTIONS_RESPONSE = """
            {
              "accounts": [
                {
                  "id": "acct-123",
                  "name": "My Checking",
                  "balance": "1234.56",
                  "transactions": [
                    {
                      "id": "txn-001",
                      "posted": 1738368000,
                      "amount": "-45.99",
                      "payee": "STARBUCKS #1234",
                      "memo": "Coffee purchase",
                      "pending": false
                    },
                    {
                      "id": "txn-002",
                      "posted": 1738454400,
                      "amount": "2500.00",
                      "payee": "EMPLOYER INC",
                      "description": "Direct deposit",
                      "memo": "",
                      "pending": false
                    },
                    {
                      "id": "txn-003",
                      "posted": 1738540800,
                      "amount": "0.00",
                      "payee": "",
                      "memo": "Zero amount test",
                      "pending": false
                    }
                  ]
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new SimpleFINProvider(builder, mock(CredentialResolver.class));
    }

    // --- claimSetupToken ---

    private static final String CLAIM_URL = "https://beta-bridge.simplefin.org/simplefin/claim/abc123";
    private static final String SETUP_TOKEN = Base64.getEncoder()
            .encodeToString(CLAIM_URL.getBytes(StandardCharsets.UTF_8));

    @Test
    void claimSetupToken_decodesTokenAndPostsToDecodedUrl() {
        mockServer.expect(requestTo(CLAIM_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "https://user:pass@beta-bridge.simplefin.org/simplefin",
                        MediaType.TEXT_PLAIN));

        String accessUrl = provider.claimSetupToken(SETUP_TOKEN);

        assertEquals("https://user:pass@beta-bridge.simplefin.org/simplefin", accessUrl);
        mockServer.verify();
    }

    @Test
    void claimSetupToken_malformedToken_throwsAuthException() {
        assertThrows(SimpleFINProvider.ProviderAuthException.class,
                () -> provider.claimSetupToken("!!!not-base64!!!"));
    }

    @Test
    void claimSetupToken_alreadyUsed_throwsAuthException() {
        mockServer.expect(requestTo(CLAIM_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(SimpleFINProvider.ProviderAuthException.class,
                () -> provider.claimSetupToken(SETUP_TOKEN));
    }

    @Test
    void claimSetupToken_connectionFailure_throws() {
        mockServer.expect(requestTo(CLAIM_URL))
                .andRespond(withServerError());

        assertThrows(SimpleFINProvider.ProviderConnectionException.class,
                () -> provider.claimSetupToken(SETUP_TOKEN));
    }

    // --- fetchAccounts ---

    @Test
    void fetchAccounts_mapsCorrectly() {
        mockServer.expect(requestTo(ACCESS_URL + "/accounts"))
                .andRespond(withSuccess(ACCOUNTS_RESPONSE, MediaType.APPLICATION_JSON));

        List<ProviderAccount> accounts = provider.fetchAccounts(CREDENTIALS);

        assertEquals(2, accounts.size());
        assertEquals("acct-123", accounts.get(0).accountId());
        assertEquals("My Checking", accounts.get(0).accountName());
        assertEquals(123456, accounts.get(0).balanceCents());
        assertEquals("acct-456", accounts.get(1).accountId());
        assertEquals(-5000, accounts.get(1).balanceCents());
    }

    @Test
    void fetchAccounts_nullCredentials_throws() {
        assertThrows(SimpleFINProvider.ProviderAuthException.class,
                () -> provider.fetchAccounts(null));
    }

    // --- fetchTransactions ---

    @Test
    void fetchTransactions_mapsAmountsAndTypes() {
        mockServer.expect(requestToUriTemplate(
                        ACCESS_URL + "/accounts?account={account}&start-date={start}",
                        "acct-123", "1738281600"))
                .andRespond(withSuccess(TRANSACTIONS_RESPONSE, MediaType.APPLICATION_JSON));

        List<ProviderTransaction> txns = provider.fetchTransactions(
                CREDENTIALS, "acct-123", LocalDate.of(2025, 1, 31), null);

        assertEquals(3, txns.size());

        // Debit: negative amount
        ProviderTransaction debit = txns.get(0);
        assertEquals("txn-001", debit.transactionId());
        assertEquals(4599, debit.amountCents());
        assertEquals(TransactionType.DEBIT, debit.transactionType());
        assertEquals("STARBUCKS #1234", debit.merchantName());
        assertEquals("Coffee purchase", debit.description());

        // Credit: positive amount
        ProviderTransaction credit = txns.get(1);
        assertEquals("txn-002", credit.transactionId());
        assertEquals(250000, credit.amountCents());
        assertEquals(TransactionType.CREDIT, credit.transactionType());
        assertEquals("EMPLOYER INC", credit.merchantName());

        // Zero amount: treated as credit
        ProviderTransaction zero = txns.get(2);
        assertEquals(0, zero.amountCents());
        assertEquals(TransactionType.CREDIT, zero.transactionType());
    }

    @Test
    void fetchTransactions_mapsUnixTimestampsToLocalDate() {
        mockServer.expect(anything())
                .andRespond(withSuccess(TRANSACTIONS_RESPONSE, MediaType.APPLICATION_JSON));

        List<ProviderTransaction> txns = provider.fetchTransactions(
                CREDENTIALS, "acct-123", LocalDate.of(2025, 1, 1), null);

        // 1738368000 = 2025-02-01 UTC
        assertEquals(LocalDate.of(2025, 2, 1), txns.get(0).date());
    }

    // --- toCents ---

    @Test
    void toCents_handlesVariousFormats() {
        assertEquals(-4599, SimpleFINProvider.toCents("-45.99"));
        assertEquals(250000, SimpleFINProvider.toCents("2500.00"));
        assertEquals(0, SimpleFINProvider.toCents("0.00"));
        assertEquals(100, SimpleFINProvider.toCents("1"));
        assertEquals(50, SimpleFINProvider.toCents("0.5"));
        assertEquals(1, SimpleFINProvider.toCents("0.009"));
    }
}
