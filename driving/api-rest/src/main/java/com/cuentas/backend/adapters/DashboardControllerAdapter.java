package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;
import com.cuentas.backend.domain.PeriodSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/dashboard")
public class DashboardControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DashboardControllerAdapter.class);

    private final DashboardServicePort dashboardService;

    public DashboardControllerAdapter(DashboardServicePort dashboardService) {
        this.dashboardService = dashboardService;
    }

    // =======================
    // MÉTRICAS PERSONALIZADAS
    // =======================

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

    /**
     * Retorna el resumen de ingresos, gastos y beneficio neto del usuario
     * para el mes actual, mes anterior o año actual.
     */
    @GetMapping("/summary")
    public ResponseEntity<PeriodSummary> getPeriodSummary(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "month") String period // "month", "lastMonth", "year"
    ) {
        logger.info("Obteniendo resumen financiero para userId={} en periodo={}", userId, period);
        PeriodSummary summary = dashboardService.getPeriodSummary(userId, period);
        logger.info("Respuesta getPeriodSummary: {}", summary);
        return ResponseEntity.ok(summary);
    }

    /**
     * Devuelve la evolución mensual del usuario durante un año.
     */
    @GetMapping("/summary/monthly")
    public ResponseEntity<List<PeriodSummary>> getMonthlySummary(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year
    ) {
        logger.info("Obteniendo evolución mensual de ingresos/gastos para userId={}, year={}", userId, year);
        List<PeriodSummary> summaries = dashboardService.getMonthlySummary(userId, year);
        return ResponseEntity.ok(summaries);
    }

    // =======================
    // MÉTRICAS DETALLADAS
    // =======================

    @GetMapping("/assets/{assetId}/performance")
    public ResponseEntity<AssetPerformance> getAssetPerformance(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        logger.info("Obteniendo performance de assetId={} para userId={}, startDate={}, endDate={}", assetId, userId, startDate, endDate);
        AssetPerformance performance = dashboardService.getAssetPerformance(userId, assetId, startDate, endDate);
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/liabilities/{liabilityId}/progress")
    public ResponseEntity<LiabilityProgress> getLiabilityProgress(
            @PathVariable Long userId,
            @PathVariable Long liabilityId
    ) {
        logger.info("Obteniendo progreso de liabilityId={} para userId={}", liabilityId, userId);
        LiabilityProgress progress = dashboardService.getLiabilityProgress(userId, liabilityId);
        return ResponseEntity.ok(progress);
    }
}
