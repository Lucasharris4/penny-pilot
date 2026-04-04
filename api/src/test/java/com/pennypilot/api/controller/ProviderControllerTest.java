package com.pennypilot.api.controller;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.ProviderProperties;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.entity.Provider;
import com.pennypilot.api.entity.ProviderType;
import com.pennypilot.api.repository.ProviderRepository;
import com.pennypilot.api.service.JwtService;
import com.pennypilot.api.util.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProviderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ProviderControllerTest.TestConfig.class})
class ProviderControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtService jwtService() {
            AuthProperties props = new AuthProperties(8, "test-secret-key-that-is-long-enough-for-hmac-sha", 86400000L);
            return new JwtService(props, new FixedClock(Instant.now()));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderRepository providerRepository;

    @MockitoBean
    private ProviderProperties providerProperties;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "test@example.com");
    }

    private Provider makeProvider(Long id, ProviderType name, String description) {
        Provider p = new Provider();
        p.setId(id);
        p.setName(name);
        p.setDescription(description);
        return p;
    }

    @Test
    void listProviders_mockEnabled_returnsAll() throws Exception {
        when(providerProperties.mockEnabled()).thenReturn(true);
        when(providerRepository.findAll()).thenReturn(List.of(
                makeProvider(1L, ProviderType.MOCK, "Sandbox provider with sample data"),
                makeProvider(2L, ProviderType.SIMPLEFIN, "SimpleFIN Bridge")
        ));

        mockMvc.perform(get("/api/providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("MOCK"))
                .andExpect(jsonPath("$[1].name").value("SIMPLEFIN"));
    }

    @Test
    void listProviders_mockDisabled_filtersMock() throws Exception {
        when(providerProperties.mockEnabled()).thenReturn(false);
        when(providerRepository.findAll()).thenReturn(List.of(
                makeProvider(1L, ProviderType.MOCK, "Sandbox provider with sample data"),
                makeProvider(2L, ProviderType.SIMPLEFIN, "SimpleFIN Bridge")
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
