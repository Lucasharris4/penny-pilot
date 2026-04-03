package com.pennypilot.api.controller;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.dto.account.AccountResponse;
import com.pennypilot.api.dto.account.LinkAccountsRequest;
import com.pennypilot.api.service.AccountService;
import com.pennypilot.api.service.JwtService;
import com.pennypilot.api.util.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AccountControllerTest.JwtTestConfig.class})
class AccountControllerTest {

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
    private AccountService accountService;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "user@example.com");
    }

    // --- link ---

    @Test
    void linkAccounts_returns201() throws Exception {
        Instant now = Instant.now();
        when(accountService.linkAccounts(eq(1L), eq(1L))).thenReturn(List.of(
                new AccountResponse(1L, 1L, "MOCK", "acct-001", "Checking", 150000, null),
                new AccountResponse(2L, 1L, "MOCK", "acct-002", "Savings", 500000, null)
        ));

        mockMvc.perform(post("/api/accounts/link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accountName").value("Checking"))
                .andExpect(jsonPath("$[0].providerName").value("MOCK"))
                .andExpect(jsonPath("$[0].balanceCents").value(150000))
                .andExpect(jsonPath("$[1].accountName").value("Savings"));
    }

    @Test
    void linkAccounts_nullProviderId_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts/link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkAccounts_alreadyLinked_returns409() throws Exception {
        when(accountService.linkAccounts(eq(1L), eq(1L)))
                .thenThrow(new AccountService.AccountsAlreadyLinkedException());

        mockMvc.perform(post("/api/accounts/link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void linkAccounts_providerNotFound_returns404() throws Exception {
        when(accountService.linkAccounts(eq(1L), eq(99L)))
                .thenThrow(new AccountService.ProviderNotFoundException(99L));

        mockMvc.perform(post("/api/accounts/link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": 99}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void linkAccounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/accounts/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": 1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- list ---

    @Test
    void listAccounts_returns200() throws Exception {
        when(accountService.listAccounts(1L)).thenReturn(List.of(
                new AccountResponse(1L, 1L, "MOCK", "acct-001", "Checking", 150000, null)
        ));

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountName").value("Checking"));
    }

    @Test
    void listAccounts_empty_returns200() throws Exception {
        when(accountService.listAccounts(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listAccounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    // --- delete ---

    @Test
    void deleteAccount_returns204() throws Exception {
        mockMvc.perform(delete("/api/accounts/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAccount_notFound_returns404() throws Exception {
        doThrow(new AccountService.AccountNotFoundException(99L))
                .when(accountService).deleteAccount(1L, 99L);

        mockMvc.perform(delete("/api/accounts/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteAccount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/accounts/1"))
                .andExpect(status().isUnauthorized());
    }
}
