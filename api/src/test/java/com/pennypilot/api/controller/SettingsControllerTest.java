package com.pennypilot.api.controller;

import com.pennypilot.api.config.JwtAuthenticationFilter;
import com.pennypilot.api.config.SecurityConfig;
import com.pennypilot.api.config.TestJwtConfig;
import com.pennypilot.api.dto.auth.ChangePasswordRequest;
import com.pennypilot.api.service.AuthService;
import com.pennypilot.api.service.JwtService;
import com.pennypilot.api.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TestJwtConfig.class})
@ActiveProfiles("test")
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SettingsService settingsService;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(1L, "user@example.com");
    }

    // --- change password ---

    @Test
    void changePassword_returns200() throws Exception {
        mockMvc.perform(put("/api/settings/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "oldPass123", "newPassword": "newPass456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated"));

        verify(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        doThrow(new AuthService.InvalidCredentialsException())
                .when(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/settings/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "wrongPass", "newPassword": "newPass456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void changePassword_shortNewPassword_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Password must be at least 8 characters"))
                .when(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/settings/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "oldPass123", "newPassword": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void changePassword_missingFields_returns400() throws Exception {
        mockMvc.perform(put("/api/settings/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "oldPass123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "oldPass123", "newPassword": "newPass456"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- SimpleFIN status ---

    @Test
    void getSimpleFINStatus_returnsTrue() throws Exception {
        when(settingsService.hasSimpleFINCredentials(1L)).thenReturn(true);

        mockMvc.perform(get("/api/settings/simplefin-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasToken").value(true));
    }

    @Test
    void getSimpleFINStatus_returnsFalse() throws Exception {
        when(settingsService.hasSimpleFINCredentials(1L)).thenReturn(false);

        mockMvc.perform(get("/api/settings/simplefin-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasToken").value(false));
    }

    @Test
    void getSimpleFINStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/settings/simplefin-status"))
                .andExpect(status().isUnauthorized());
    }

    // --- update SimpleFIN token ---

    @Test
    void updateSimpleFINToken_returns200() throws Exception {
        mockMvc.perform(put("/api/settings/simplefin-token")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"setupToken": "new-setup-token-abc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SimpleFIN token updated"));

        verify(settingsService).updateSimpleFINToken(1L, "new-setup-token-abc");
    }

    @Test
    void updateSimpleFINToken_blankToken_returns400() throws Exception {
        mockMvc.perform(put("/api/settings/simplefin-token")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"setupToken": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSimpleFINToken_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/settings/simplefin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"setupToken": "token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- delete SimpleFIN token ---

    @Test
    void deleteSimpleFINToken_returns204() throws Exception {
        mockMvc.perform(delete("/api/settings/simplefin-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(settingsService).deleteSimpleFINCredentials(1L);
    }

    @Test
    void deleteSimpleFINToken_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/settings/simplefin-token"))
                .andExpect(status().isUnauthorized());
    }
}
