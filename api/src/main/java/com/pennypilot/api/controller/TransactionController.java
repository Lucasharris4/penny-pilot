package com.pennypilot.api.controller;

import com.pennypilot.api.config.SecurityUtils;
import com.pennypilot.api.dto.transaction.*;
import com.pennypilot.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "List transactions with filtering, sorting, and pagination")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved")
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minAmount,
            @RequestParam(required = false) Integer maxAmount,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "date") Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<TransactionResponse> page = transactionService.listTransactions(
                userId, startDate, endDate, categoryId, minAmount, maxAmount, search, pageable);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a transaction")
    @ApiResponse(responseCode = "200", description = "Transaction updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bulk-categorize")
    @Operation(summary = "Assign a category to multiple transactions")
    @ApiResponse(responseCode = "200", description = "Transactions categorized")
    @ApiResponse(responseCode = "400", description = "Invalid transaction IDs")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<BulkCategorizeResponse> bulkCategorize(
            @Valid @RequestBody BulkCategorizeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        BulkCategorizeResponse response = transactionService.bulkCategorize(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get spending summary by category for a time range")
    @ApiResponse(responseCode = "200", description = "Summary retrieved")
    public ResponseEntity<List<TransactionSummaryResponse>> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TransactionSummaryResponse> summary = transactionService.getSummary(userId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @ExceptionHandler(TransactionService.TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TransactionService.TransactionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TransactionService.CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(TransactionService.CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TransactionService.InvalidTransactionIdsException.class)
    public ResponseEntity<BulkCategorizeResponse> handleInvalidIds(TransactionService.InvalidTransactionIdsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BulkCategorizeResponse(0, ex.getInvalidIds()));
    }

    record ErrorResponse(String message) {}
}
