package com.pennypilot.api.controller;

import com.pennypilot.api.config.SecurityUtils;
import com.pennypilot.api.dto.account.AccountResponse;
import com.pennypilot.api.dto.account.LinkAccountsRequest;
import com.pennypilot.api.dto.sync.SyncResponse;
import com.pennypilot.api.provider.ProviderResolver;
import com.pennypilot.api.provider.SimpleFINProvider;
import com.pennypilot.api.service.AccountService;
import com.pennypilot.api.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    private final AccountService accountService;
    private final SyncService syncService;

    public AccountController(AccountService accountService, SyncService syncService) {
        this.accountService = accountService;
        this.syncService = syncService;
    }

    @PostMapping("/link")
    @Operation(summary = "Link accounts from a provider")
    @ApiResponse(responseCode = "201", description = "Accounts linked")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Provider not found")
    @ApiResponse(responseCode = "409", description = "Accounts already linked")
    public ResponseEntity<List<AccountResponse>> linkAccounts(@Valid @RequestBody LinkAccountsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AccountResponse> accounts = accountService.linkAccounts(userId, request.providerId(), request.setupToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts);
    }

    @GetMapping
    @Operation(summary = "List all accounts for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Accounts retrieved")
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(accountService.listAccounts(userId));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Sync transactions for an account")
    @ApiResponse(responseCode = "200", description = "Sync completed")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "502", description = "Provider error")
    public ResponseEntity<SyncResponse> syncAccount(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        SyncResponse response = syncService.syncAccount(userId, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an account and its transactions")
    @ApiResponse(responseCode = "204", description = "Account deleted")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        accountService.deleteAccount(userId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AccountService.AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AccountService.AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SyncService.AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSyncAccountNotFound(SyncService.AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccountService.AccountsAlreadyLinkedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyLinked(AccountService.AccountsAlreadyLinkedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccountService.ProviderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProviderNotFound(AccountService.ProviderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ProviderResolver.ProviderNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleProviderNotSupported(ProviderResolver.ProviderNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccountService.SetupTokenRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSetupTokenRequired(AccountService.SetupTokenRequiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SimpleFINProvider.ProviderAuthException.class)
    public ResponseEntity<ErrorResponse> handleProviderAuth(SimpleFINProvider.ProviderAuthException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SimpleFINProvider.ProviderConnectionException.class)
    public ResponseEntity<ErrorResponse> handleProviderConnection(SimpleFINProvider.ProviderConnectionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getMessage()));
    }

    record ErrorResponse(String message) {}
}
