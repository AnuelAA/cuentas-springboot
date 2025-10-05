package com.cuentas.backend.application.ports.driving;


import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;

import java.time.LocalDate;

public interface DashboardServicePort {

    DashboardMetrics getMetrics(Long userId, LocalDate startDate, LocalDate endDate);
    AssetPerformance getAssetPerformance(Long userId, Long assetId, LocalDate startDate, LocalDate endDate);
    LiabilityProgress getLiabilityProgress(Long userId, Long liabilityId);
}
