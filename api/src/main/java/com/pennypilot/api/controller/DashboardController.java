package com.pennypilot.api.controller;

import com.pennypilot.api.config.SecurityUtils;
import com.pennypilot.api.dto.dashboard.AvailableMonthsResponse;
import com.pennypilot.api.dto.dashboard.DashboardSummaryResponse;
import com.pennypilot.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard data endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary with totals and category breakdown")
    @ApiResponse(responseCode = "200", description = "Summary retrieved")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getSummary(userId, startDate, endDate));
    }

    @GetMapping("/available-months")
    @Operation(summary = "Get months that have transaction data")
    @ApiResponse(responseCode = "200", description = "Available months retrieved")
    public ResponseEntity<AvailableMonthsResponse> getAvailableMonths() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getAvailableMonths(userId));
    }
}
