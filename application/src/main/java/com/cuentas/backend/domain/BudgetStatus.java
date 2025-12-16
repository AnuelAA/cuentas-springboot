package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatus {
    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private Double budgetAmount;
    private Double spentAmount;
    private Double remainingAmount;
    private Double percentageUsed;
    private Boolean isExceeded;
    private String period; // "monthly" or "yearly"
    private LocalDate startDate;
    private LocalDate endDate;
}

