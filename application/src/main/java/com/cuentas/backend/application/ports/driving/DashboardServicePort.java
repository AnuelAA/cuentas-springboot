package com.cuentas.backend.application.ports.driving;


import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;

import java.time.LocalDate;

public interface DashboardServicePort {

    /**
     * Obtiene métricas agregadas de un usuario entre dos fechas
     * @param userId ID del usuario
     * @param startDate Fecha de inicio (inclusive)
     * @param endDate Fecha final (inclusive)
     * @return DashboardMetrics con ingresos, gastos y beneficio neto
     */
    DashboardMetrics getMetrics(Long userId, LocalDate startDate, LocalDate endDate);
    AssetPerformance getAssetPerformance(Long userId, Long assetId, LocalDate startDate, LocalDate endDate);
    LiabilityProgress getLiabilityProgress(Long userId, Long liabilityId);
}
