package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.DashboardMetrics;
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
}
