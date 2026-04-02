package com.pennypilot.api.controller;

import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.dto.category.CategoryRuleResponse;
import com.pennypilot.api.dto.category.CreateCategoryRuleRequest;
import com.pennypilot.api.dto.category.UpdateCategoryRuleRequest;
import com.pennypilot.api.service.CategoryRuleService;
import com.pennypilot.api.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryRuleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoryRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRuleService categoryRuleService;

    @MockitoBean
    private JwtService jwtService;

    private static final String FAKE_TOKEN = "fake-jwt-token";

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(jwtService.isValid(FAKE_TOKEN)).thenReturn(true);
        when(jwtService.parseToken(FAKE_TOKEN)).thenReturn(claims);
    }

    // --- list ---

    @Test
    void listRules_returns200() throws Exception {
        when(categoryRuleService.listRules(1L)).thenReturn(List.of(
                new CategoryRuleResponse(1L, "STARBUCKS*", 5L, "Coffee", 10)
        ));

        mockMvc.perform(get("/api/category-rules")
                        .header("Authorization", "Bearer " + FAKE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matchPattern").value("STARBUCKS*"))
                .andExpect(jsonPath("$[0].categoryName").value("Coffee"));
    }

    @Test
    void listRules_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/category-rules"))
                .andExpect(status().isUnauthorized());
    }

    // --- create ---

    @Test
    void createRule_returns201() throws Exception {
        when(categoryRuleService.createRule(eq(1L), any(CreateCategoryRuleRequest.class)))
                .thenReturn(new CategoryRuleResponse(1L, "STARBUCKS*", 5L, "Coffee", 10));

        mockMvc.perform(post("/api/category-rules")
                        .header("Authorization", "Bearer " + FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchPattern": "STARBUCKS*", "categoryId": 5, "priority": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.matchPattern").value("STARBUCKS*"));
    }

    @Test
    void createRule_blankPattern_returns400() throws Exception {
        mockMvc.perform(post("/api/category-rules")
                        .header("Authorization", "Bearer " + FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchPattern": "", "categoryId": 5, "priority": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRule_missingCategoryId_returns400() throws Exception {
        mockMvc.perform(post("/api/category-rules")
                        .header("Authorization", "Bearer " + FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchPattern": "STARBUCKS*", "priority": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRule_categoryNotFound_returns404() throws Exception {
        when(categoryRuleService.createRule(eq(1L), any(CreateCategoryRuleRequest.class)))
                .thenThrow(new CategoryRuleService.CategoryNotFoundException(99L));

        mockMvc.perform(post("/api/category-rules")
                        .header("Authorization", "Bearer " + FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchPattern": "STARBUCKS*", "categoryId": 99, "priority": 10}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // --- update ---

    @Test
    void updateRule_returns200() throws Exception {
        when(categoryRuleService.updateRule(eq(1L), eq(1L), any(UpdateCategoryRuleRequest.class)))
                .thenReturn(new CategoryRuleResponse(1L, "DUNKIN*", 6L, "Dining", 5));

        mockMvc.perform(put("/api/category-rules/1")
                        .header("Authorization", "Bearer " + FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchPattern": "DUNKIN*", "categoryId": 6, "priority": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPattern").value("DUNKIN*"));
    }

    @Test
    void updateRule_notFound_returns404() throws Exception {
        when(categoryRuleService.updateRule(eq(1L), eq(99L), any(UpdateCategoryRuleRequest.class)))
                .thenThrow(new CategoryRuleService.RuleNotFoundException(99L));

        mockMvc.perform(put("/api/category-rules/99")
                        .header("Authorization", "Bearer " + FAKE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchPattern": "DUNKIN*", "categoryId": 5, "priority": 5}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // --- delete ---

    @Test
    void deleteRule_returns204() throws Exception {
        mockMvc.perform(delete("/api/category-rules/1")
                        .header("Authorization", "Bearer " + FAKE_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRule_notFound_returns404() throws Exception {
        doThrow(new CategoryRuleService.RuleNotFoundException(99L))
                .when(categoryRuleService).deleteRule(1L, 99L);

        mockMvc.perform(delete("/api/category-rules/99")
                        .header("Authorization", "Bearer " + FAKE_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
