package com.pennypilot.api.controller;

import com.pennypilot.api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    void authEndpoints_noAuth_notBlocked() throws Exception {
        mockMvc.perform(get("/api/auth/register"))
                // GET not mapped, but should not be 401 — auth endpoints are permitted
                // Verify it's 405 (method not allowed) not 401 (unauthorized)
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void nonExistentProtectedEndpoint_noAuth_returns401() throws Exception {
        // Intentionally fake endpoint to verify the filter rejects unauthenticated requests
        mockMvc.perform(get("/api/this-endpoint-does-not-exist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonExistentProtectedEndpoint_validToken_returns404() throws Exception {
        // With a valid token the filter passes, but the endpoint doesn't exist — so 404
        String token = jwtService.generateToken(1L, "user@example.com");

        mockMvc.perform(get("/api/this-endpoint-does-not-exist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonExistentProtectedEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/this-endpoint-does-not-exist")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void realEndpoint_validToken_succeeds() throws Exception {
        // Register a user, then login to get a real token, then use it
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "filter-test@example.com", "password": "password123"}
                                """))
                .andExpect(status().isCreated());

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "filter-test@example.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract token and verify it works against the logout endpoint (a real, authenticated-compatible endpoint)
        String token = responseBody.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
