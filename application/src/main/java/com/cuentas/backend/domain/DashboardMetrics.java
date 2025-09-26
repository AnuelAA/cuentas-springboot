package com.cuentas.backend.domain;

import java.math.BigDecimal;

public class DashboardMetrics {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;

    public DashboardMetrics() {
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpense = BigDecimal.ZERO;
        this.netProfit = BigDecimal.ZERO;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }
}
