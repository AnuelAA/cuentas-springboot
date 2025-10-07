package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetROI {
    private Long assetId;
    private double totalIncome;
    private double totalExpenses;
    private double netProfit;
    private double roiPercentage;
}