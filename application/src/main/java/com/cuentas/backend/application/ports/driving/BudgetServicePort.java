package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Budget;
import com.cuentas.backend.domain.BudgetStatus;
import java.util.List;

public interface BudgetServicePort {
    Budget createBudget(Long userId, Budget budget);
    Budget getBudget(Long userId, Long budgetId);
    List<Budget> listBudgets(Long userId);
    Budget updateBudget(Long userId, Long budgetId, Budget budget);
    void deleteBudget(Long userId, Long budgetId);
    BudgetStatus getBudgetStatus(Long userId);
}

