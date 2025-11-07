package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDetail {
    private Asset asset;
    private BigDecimal currentValue; // Último valor registrado
    private BigDecimal totalIncome; // Ingresos generados por este activo
    private BigDecimal totalExpenses; // Gastos asociados a este activo
    private BigDecimal netProfit; // Beneficio neto
    private Double roiPercentage; // ROI en porcentaje
    private Integer transactionCount;
    private List<Transaction> recentTransactions; // Últimas 10 transacciones
    private List<AssetValue> valueHistory; // Historial de valores (últimos 12 meses)
}

