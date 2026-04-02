package com.pennypilot.api.controller;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void healthEndpoint_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authEndpoints_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/auth/register"))
                // GET not mapped, but should not be 401 — auth endpoints are permitted
                .andExpect(status().is4xxClientError())
                // Verify it's 405 (method not allowed) not 401 (unauthorized)
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void protectedEndpoint_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_validToken_returns404() throws Exception {
        // With a valid token, the request is authenticated but /api/categories doesn't exist yet
        // so we expect 404, not 401
        String token = jwtService.generateToken(1L, "user@example.com");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}
