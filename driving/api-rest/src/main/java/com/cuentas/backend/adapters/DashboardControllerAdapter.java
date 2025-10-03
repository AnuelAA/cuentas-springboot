package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userId}/dashboard")
public class DashboardControllerAdapter {

    private final DashboardServicePort dashboardService;

    public DashboardControllerAdapter(DashboardServicePort dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardMetrics> getDashboard(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        DashboardMetrics metrics = dashboardService.getMetrics(userId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/assets/{assetId}/performance")
    public ResponseEntity<AssetPerformance> getAssetPerformance(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        AssetPerformance performance = dashboardService.getAssetPerformance(userId, assetId, startDate, endDate);
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/liabilities/{liabilityId}/progress")
    public ResponseEntity<LiabilityProgress> getLiabilityProgress(
            @PathVariable Long userId,
            @PathVariable Long liabilityId
    ) {
        LiabilityProgress progress = dashboardService.getLiabilityProgress(userId, liabilityId);
        return ResponseEntity.ok(progress);
    }
}
