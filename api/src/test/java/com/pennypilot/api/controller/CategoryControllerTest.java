package com.pennypilot.api.controller;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.config.FixedClock;
import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.dto.category.CategoryResponse;
import com.pennypilot.api.dto.category.CreateCategoryRequest;
import com.pennypilot.api.dto.category.UpdateCategoryRequest;
import com.pennypilot.api.service.CategoryService;
import com.pennypilot.api.service.JwtService;
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

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CategoryControllerTest.JwtTestConfig.class})
class CategoryControllerTest {

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
    private CategoryService categoryService;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "user@example.com");
    }

    // --- list ---

    @Test
    void listCategories_returns200() throws Exception {
        when(categoryService.listCategories(1L)).thenReturn(List.of(
                new CategoryResponse(1L, "Groceries", "\uD83D\uDED2", "#4CAF50"),
                new CategoryResponse(2L, "Dining", "\uD83C\uDF7D\uFE0F", "#FF9800")
        ));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Groceries"))
                .andExpect(jsonPath("$[1].name").value("Dining"));
    }

    @Test
    void listCategories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    // --- create ---

    @Test
    void createCategory_returns201() throws Exception {
        when(categoryService.createCategory(eq(1L), any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Coffee", "\u2615", "#8B4513"));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Coffee", "icon": "\\u2615", "color": "#8B4513"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Coffee"));
    }

    @Test
    void createCategory_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "icon": null, "color": null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_duplicateName_returns409() throws Exception {
        when(categoryService.createCategory(eq(1L), any(CreateCategoryRequest.class)))
                .thenThrow(new CategoryService.CategoryNameExistsException("Groceries"));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // --- update ---

    @Test
    void updateCategory_returns200() throws Exception {
        when(categoryService.updateCategory(eq(1L), eq(1L), any(UpdateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Groceries", "\uD83C\uDF4E", "#FF0000"));

        mockMvc.perform(put("/api/categories/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "icon": "\uD83C\uDF4E", "color": "#FF0000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.color").value("#FF0000"));
    }

    @Test
    void updateCategory_notFound_returns404() throws Exception {
        when(categoryService.updateCategory(eq(1L), eq(99L), any(UpdateCategoryRequest.class)))
                .thenThrow(new CategoryService.CategoryNotFoundException(99L));

        mockMvc.perform(put("/api/categories/99")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // --- delete ---

    @Test
    void deleteCategory_returns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_notFound_returns404() throws Exception {
        doThrow(new CategoryService.CategoryNotFoundException(99L))
                .when(categoryService).deleteCategory(1L, 99L);

        mockMvc.perform(delete("/api/categories/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
