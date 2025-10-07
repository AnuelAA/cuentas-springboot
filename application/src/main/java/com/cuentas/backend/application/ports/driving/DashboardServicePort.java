package com.cuentas.backend.application.ports.driving;


import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;
import com.cuentas.backend.domain.PeriodSummary;

import java.time.LocalDate;
import java.util.List;

public interface DashboardServicePort {

    DashboardMetrics getMetrics(Long userId, LocalDate startDate, LocalDate endDate);
    AssetPerformance getAssetPerformance(Long userId, Long assetId, LocalDate startDate, LocalDate endDate);
    LiabilityProgress getLiabilityProgress(Long userId, Long liabilityId);
    List<PeriodSummary> getMonthlySummary(Long userId, Integer year);
    PeriodSummary getPeriodSummary(Long userId, String period);
}
