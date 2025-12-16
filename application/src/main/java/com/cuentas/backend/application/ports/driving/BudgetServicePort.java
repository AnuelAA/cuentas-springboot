package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Budget;
import com.cuentas.backend.domain.BudgetStatus;

import java.time.LocalDate;
import java.util.List;

public interface BudgetServicePort {
    List<Budget> getBudgets(Long userId, LocalDate startDate, LocalDate endDate);
    List<BudgetStatus> getBudgetsStatus(Long userId, LocalDate startDate, LocalDate endDate);
    Budget createBudget(Long userId, Budget budget);
    Budget updateBudget(Long userId, Long budgetId, Budget budget);
    void deleteBudget(Long userId, Long budgetId);
}

