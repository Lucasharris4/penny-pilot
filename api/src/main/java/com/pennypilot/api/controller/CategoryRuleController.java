package com.pennypilot.api.controller;

import com.pennypilot.api.config.SecurityUtils;
import com.pennypilot.api.dto.CategoryRuleResponse;
import com.pennypilot.api.dto.CreateCategoryRuleRequest;
import com.pennypilot.api.dto.UpdateCategoryRuleRequest;
import com.pennypilot.api.service.CategoryRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-rules")
@Tag(name = "Category Rules", description = "Category rule management endpoints")
public class CategoryRuleController {

    private final CategoryRuleService categoryRuleService;

    public CategoryRuleController(CategoryRuleService categoryRuleService) {
        this.categoryRuleService = categoryRuleService;
    }

    @GetMapping
    @Operation(summary = "List all category rules for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rules retrieved")
    public ResponseEntity<List<CategoryRuleResponse>> listRules() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryRuleService.listRules(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new category rule")
    @ApiResponse(responseCode = "201", description = "Rule created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryRuleResponse> createRule(@Valid @RequestBody CreateCategoryRuleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CategoryRuleResponse rule = categoryRuleService.createRule(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category rule")
    @ApiResponse(responseCode = "200", description = "Rule updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Rule or category not found")
    public ResponseEntity<CategoryRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRuleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CategoryRuleResponse rule = categoryRuleService.updateRule(userId, id, request);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category rule")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        categoryRuleService.deleteRule(userId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(CategoryRuleService.RuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRuleNotFound(CategoryRuleService.RuleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CategoryRuleService.CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryRuleService.CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    record ErrorResponse(String message) {}
}
