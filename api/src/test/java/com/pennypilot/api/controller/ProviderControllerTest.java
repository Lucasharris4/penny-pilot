package com.pennypilot.api.controller;

import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.config.TestJwtConfig;
import com.pennypilot.api.dto.provider.ProviderResponse;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.service.JwtService;
import com.pennypilot.api.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProviderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TestJwtConfig.class})
@ActiveProfiles("test")
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderService providerService;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "test@example.com");
    }

    @Test
    void listProviders_returnsServiceResult() throws Exception {
        when(providerService.listAvailableProviders()).thenReturn(List.of(
                new ProviderResponse(2L, ProviderType.SIMPLEFIN, "SimpleFIN Bridge")
        ));

        mockMvc.perform(get("/api/providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("SIMPLEFIN"));
    }

    @Test
    void listProviders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isUnauthorized());
    }
}
