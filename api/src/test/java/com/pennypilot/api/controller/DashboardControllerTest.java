package com.pennypilot.api.controller;

import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.config.TestJwtConfig;
import com.pennypilot.api.dto.dashboard.AvailableMonthsResponse;
import com.pennypilot.api.dto.dashboard.CategoryBreakdown;
import com.pennypilot.api.dto.dashboard.DashboardSummaryResponse;
import com.pennypilot.api.service.DashboardService;
import com.pennypilot.api.service.JwtService;
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

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TestJwtConfig.class})
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "test@example.com");
    }

    @Test
    void getSummary_returnsResponseShape() throws Exception {
        when(dashboardService.getSummary(1L, "2026-03-01", "2026-03-31")).thenReturn(
                new DashboardSummaryResponse(500000L, 300000L, 200000L, List.of(
                        new CategoryBreakdown(10L, "Groceries", "#4CAF50", 180000L, 60.0),
                        new CategoryBreakdown(20L, "Dining", "#FF9800", 120000L, 40.0)
                ))
        );

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeCents").value(500000))
                .andExpect(jsonPath("$.expensesCents").value(300000))
                .andExpect(jsonPath("$.netCents").value(200000))
                .andExpect(jsonPath("$.byCategory.length()").value(2))
                .andExpect(jsonPath("$.byCategory[0].categoryName").value("Groceries"));
    }

    @Test
    void getSummary_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAvailableMonths_returnsMonths() throws Exception {
        when(dashboardService.getAvailableMonths(1L)).thenReturn(
                new AvailableMonthsResponse(List.of("2026-04", "2026-03"))
        );

        mockMvc.perform(get("/api/dashboard/available-months")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months.length()").value(2))
                .andExpect(jsonPath("$.months[0]").value("2026-04"));
    }

    @Test
    void getAvailableMonths_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/available-months"))
                .andExpect(status().isUnauthorized());
    }
}
