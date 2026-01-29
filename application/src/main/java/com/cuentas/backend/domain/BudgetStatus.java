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
public class BudgetStatus {
    private List<BudgetStatusItem> items;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal totalRemaining;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetStatusItem {
        private Long budgetId;
        private Long categoryId;
        private String categoryName;
        private BigDecimal budgetAmount;
        private BigDecimal spent;
        private BigDecimal remaining;
        private Double percentageUsed;
        private String period;
    }
}

