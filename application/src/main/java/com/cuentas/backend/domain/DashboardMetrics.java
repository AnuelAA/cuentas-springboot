package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

    private BigDecimal totalIncome;     // Suma de ingresos en el periodo
    private BigDecimal totalExpenses;   // Suma de gastos en el periodo
    private BigDecimal netBalance;      // Diferencia: ingresos - gastos
    private Asset bestAsset;            // Activo con mayor valor
    private Asset worstAsset;           // Activo con menor valor
}