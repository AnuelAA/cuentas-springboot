package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyROI {
    private String month; // formato "YYYY-MM"
    private double income;
    private double expenses;
    private double netProfit;
    private double roiPercentage;
}