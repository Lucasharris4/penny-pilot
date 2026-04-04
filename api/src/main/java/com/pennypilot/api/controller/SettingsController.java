package com.pennypilot.api.controller;

import com.pennypilot.api.dto.auth.ChangePasswordRequest;
import com.pennypilot.api.service.AuthService;
import com.pennypilot.api.service.SettingsService;
import com.pennypilot.api.config.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "User settings management")
public class SettingsController {

    private final AuthService authService;
    private final SettingsService settingsService;

    public SettingsController(AuthService authService, SettingsService settingsService) {
        this.authService = authService;
        this.settingsService = settingsService;
    }

    @PutMapping("/password")
    @Operation(summary = "Change the authenticated user's password")
    @ApiResponse(responseCode = "200", description = "Password updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Current password incorrect")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(new MessageResponse("Password updated"));
    }

    @GetMapping("/simplefin-status")
    @Operation(summary = "Check if SimpleFIN credentials are saved")
    @ApiResponse(responseCode = "200", description = "Status returned")
    public ResponseEntity<SimpleFINStatusResponse> getSimpleFINStatus() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean hasToken = settingsService.hasSimpleFINCredentials(userId);
        return ResponseEntity.ok(new SimpleFINStatusResponse(hasToken));
    }

    @PutMapping("/simplefin-token")
    @Operation(summary = "Save or replace SimpleFIN setup token")
    @ApiResponse(responseCode = "200", description = "Token saved")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<MessageResponse> updateSimpleFINToken(
            @Valid @RequestBody UpdateSimpleFINTokenRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        settingsService.updateSimpleFINToken(userId, request.setupToken());
        return ResponseEntity.ok(new MessageResponse("SimpleFIN token updated"));
    }

    @DeleteMapping("/simplefin-token")
    @Operation(summary = "Remove SimpleFIN credentials")
    @ApiResponse(responseCode = "204", description = "Credentials removed")
    public ResponseEntity<Void> deleteSimpleFINToken() {
        Long userId = SecurityUtils.getCurrentUserId();
        settingsService.deleteSimpleFINCredentials(userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<MessageResponse> handleInvalidCredentials(AuthService.InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new MessageResponse(ex.getMessage()));
    }

    record MessageResponse(String message) {}
    record SimpleFINStatusResponse(boolean hasToken) {}
    record UpdateSimpleFINTokenRequest(
            @jakarta.validation.constraints.NotBlank(message = "Setup token is required")
            String setupToken
    ) {}
}
