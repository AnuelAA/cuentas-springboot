package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userId}/dashboard")
public class DashboardControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DashboardControllerAdapter.class);

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
        logger.info("Obteniendo dashboard para userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        DashboardMetrics metrics = dashboardService.getMetrics(userId, startDate, endDate);
        logger.info("Respuesta getDashboard: {}", metrics);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/assets/{assetId}/performance")
    public ResponseEntity<AssetPerformance> getAssetPerformance(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        logger.info("Obteniendo performance de assetId={} para userId={}, startDate={}, endDate={}", assetId, userId, startDate, endDate);
        AssetPerformance performance = dashboardService.getAssetPerformance(userId, assetId, startDate, endDate);
        logger.info("Respuesta getAssetPerformance: {}", performance);
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/liabilities/{liabilityId}/progress")
    public ResponseEntity<LiabilityProgress> getLiabilityProgress(
            @PathVariable Long userId,
            @PathVariable Long liabilityId
    ) {
        logger.info("Obteniendo progreso de liabilityId={} para userId={}", liabilityId, userId);
        LiabilityProgress progress = dashboardService.getLiabilityProgress(userId, liabilityId);
        logger.info("Respuesta getLiabilityProgress: {}", progress);
        return ResponseEntity.ok(progress);
    }
}
