package com.pennypilot.api.controller;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.config.FixedClock;
import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.dto.transaction.*;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.service.JwtService;
import com.pennypilot.api.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TransactionControllerTest.JwtTestConfig.class})
class TransactionControllerTest {

    @TestConfiguration
    static class JwtTestConfig {
        @Bean
        JwtService jwtService() {
            AuthProperties props = new AuthProperties(8, "test-secret-key-that-is-long-enough-for-hmac-sha", 86400000L);
            return new JwtService(props, new FixedClock(Instant.now()));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "user@example.com");
    }

    // --- list ---

    @Test
    void listTransactions_returns200() throws Exception {
        TransactionResponse txn = new TransactionResponse(
                1L, 1L, 5L, "Groceries", 4500, TransactionType.DEBIT,
                "WHOLE FOODS #1234", "Whole Foods", "2026-03-15", null);

        when(transactionService.listTransactions(eq(1L), any(TransactionFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.content[0].amountCents").value(4500))
                .andExpect(jsonPath("$.content[0].transactionType").value("DEBIT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listTransactions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    // --- update ---

    @Test
    void updateTransaction_returns200() throws Exception {
        TransactionResponse updated = new TransactionResponse(
                1L, 1L, 6L, "Dining", 5000, TransactionType.DEBIT,
                "NEW DESC", "New Merchant", "2026-03-20", null);

        when(transactionService.updateTransaction(eq(1L), eq(1L), any(UpdateTransactionRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/transactions/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 6, "amountCents": 5000, "transactionType": "DEBIT",
                                 "description": "NEW DESC", "merchantName": "New Merchant", "date": "2026-03-20"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Dining"))
                .andExpect(jsonPath("$.amountCents").value(5000));
    }

    @Test
    void updateTransaction_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(put("/api/transactions/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 6}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_notFound_returns404() throws Exception {
        when(transactionService.updateTransaction(eq(1L), eq(99L), any(UpdateTransactionRequest.class)))
                .thenThrow(new TransactionService.TransactionNotFoundException(99L));

        mockMvc.perform(put("/api/transactions/99")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 6, "amountCents": 5000, "transactionType": "DEBIT",
                                 "description": "DESC", "merchantName": "Merchant", "date": "2026-03-20"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // --- bulk categorize ---

    @Test
    void bulkCategorize_returns200() throws Exception {
        when(transactionService.bulkCategorize(eq(1L), any(BulkCategorizeRequest.class)))
                .thenReturn(new BulkCategorizeResponse(3));

        mockMvc.perform(put("/api/transactions/bulk-categorize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionIds": [1, 2, 3], "categoryId": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(3));
    }

    @Test
    void bulkCategorize_invalidIds_returns400() throws Exception {
        when(transactionService.bulkCategorize(eq(1L), any(BulkCategorizeRequest.class)))
                .thenThrow(new TransactionService.InvalidTransactionIdsException(List.of(99L)));

        mockMvc.perform(put("/api/transactions/bulk-categorize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionIds": [1, 99], "categoryId": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidIds[0]").value(99));
    }

    @Test
    void bulkCategorize_emptyIds_returns400() throws Exception {
        mockMvc.perform(put("/api/transactions/bulk-categorize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionIds": [], "categoryId": 5}
                                """))
                .andExpect(status().isBadRequest());
    }

}
