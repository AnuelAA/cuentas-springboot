package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.BudgetServicePort;
import com.cuentas.backend.domain.Budget;
import com.cuentas.backend.domain.BudgetStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class BudgetServiceUseCase implements BudgetServicePort {

    private final JdbcTemplate jdbcTemplate;

    public BudgetServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Budget> getBudgets(Long userId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT b.* FROM budgets b WHERE b.user_id = ?"
        );
        List<Object> params = new ArrayList<>();
        params.add(userId);

        // Filtrar por fechas si se proporcionan
        if (startDate != null && endDate != null) {
            sql.append(" AND (b.end_date IS NULL OR b.end_date >= ?) AND (b.start_date IS NULL OR b.start_date <= ?)");
            params.add(startDate);
            params.add(endDate);
        } else if (startDate != null) {
            sql.append(" AND (b.end_date IS NULL OR b.end_date >= ?)");
            params.add(startDate);
        } else if (endDate != null) {
            sql.append(" AND (b.start_date IS NULL OR b.start_date <= ?)");
            params.add(endDate);
        }

        sql.append(" ORDER BY b.category_id, b.start_date DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapRow(rs), params.toArray());
    }

    @Override
    public List<BudgetStatus> getBudgetsStatus(Long userId, LocalDate startDate, LocalDate endDate) {
        // Determinar el período a analizar
        LocalDate periodStart;
        LocalDate periodEnd;
        
        if (startDate != null && endDate != null) {
            periodStart = startDate;
            periodEnd = endDate;
        } else {
            // Por defecto, usar el mes actual
            YearMonth currentMonth = YearMonth.now();
            periodStart = currentMonth.atDay(1);
            periodEnd = currentMonth.atEndOfMonth();
        }

        // Obtener todos los presupuestos del usuario
        List<Budget> budgets = getBudgets(userId, null, null);
        List<BudgetStatus> statusList = new ArrayList<>();

        for (Budget budget : budgets) {
            BudgetStatus status = calculateBudgetStatus(budget, periodStart, periodEnd);
            if (status != null) {
                statusList.add(status);
            }
        }

        return statusList;
    }

    private BudgetStatus calculateBudgetStatus(Budget budget, LocalDate periodStart, LocalDate periodEnd) {
        // Determinar el rango de fechas para calcular gastos según el período del presupuesto
        LocalDate expenseStart;
        LocalDate expenseEnd;

        if ("monthly".equals(budget.getPeriod())) {
            // Para presupuestos mensuales, usar el mes de periodStart
            YearMonth month = YearMonth.from(periodStart);
            expenseStart = month.atDay(1);
            expenseEnd = month.atEndOfMonth();
        } else {
            // Para presupuestos anuales, usar el año de periodStart
            int year = periodStart.getYear();
            expenseStart = LocalDate.of(year, 1, 1);
            expenseEnd = LocalDate.of(year, 12, 31);
        }

        // Verificar si el presupuesto está activo en este período
        if (budget.getStartDate() != null && budget.getStartDate().isAfter(expenseEnd)) {
            return null; // Presupuesto aún no ha comenzado
        }
        if (budget.getEndDate() != null && budget.getEndDate().isBefore(expenseStart)) {
            return null; // Presupuesto ya ha terminado
        }

        // Calcular gastos de la categoría en el período
        String sqlSpent = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                "WHERE user_id = ? AND category_id = ? AND transaction_type = 'expense' " +
                "AND transaction_date >= ? AND transaction_date <= ?";
        
        Double spentAmount = jdbcTemplate.queryForObject(
                sqlSpent,
                Double.class,
                budget.getUserId(),
                budget.getCategoryId(),
                expenseStart,
                expenseEnd
        );

        if (spentAmount == null) {
            spentAmount = 0.0;
        }

        // Obtener nombre de la categoría
        String categoryName = getCategoryName(budget.getCategoryId());

        // Calcular métricas
        Double budgetAmount = budget.getAmount();
        Double remainingAmount = Math.max(0.0, budgetAmount - spentAmount);
        Double percentageUsed = budgetAmount > 0 ? (spentAmount / budgetAmount) * 100.0 : 0.0;
        Boolean isExceeded = spentAmount > budgetAmount;

        return BudgetStatus.builder()
                .budgetId(budget.getBudgetId())
                .categoryId(budget.getCategoryId())
                .categoryName(categoryName)
                .budgetAmount(budgetAmount)
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .percentageUsed(percentageUsed)
                .isExceeded(isExceeded)
                .period(budget.getPeriod())
                .startDate(expenseStart)
                .endDate(expenseEnd)
                .build();
    }

    private String getCategoryName(Long categoryId) {
        try {
            String sql = "SELECT name FROM categories WHERE category_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, categoryId);
        } catch (DataAccessException e) {
            return "Categoría desconocida";
        }
    }

    @Override
    @Transactional
    public Budget createBudget(Long userId, Budget budget) {
        // Validaciones
        if (budget.getAmount() == null || budget.getAmount() <= 0) {
            throw new IllegalArgumentException("El monto del presupuesto debe ser mayor a 0");
        }

        if (budget.getPeriod() == null || (!budget.getPeriod().equals("monthly") && !budget.getPeriod().equals("yearly"))) {
            throw new IllegalArgumentException("El período debe ser 'monthly' o 'yearly'");
        }

        // Validar que la categoría existe y pertenece al usuario
        String checkCategorySql = "SELECT category_id FROM categories WHERE category_id = ? AND user_id = ?";
        try {
            jdbcTemplate.queryForObject(checkCategorySql, Long.class, budget.getCategoryId(), userId);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("La categoría no existe o no pertenece al usuario");
        }

        // Validar fechas
        if (budget.getEndDate() != null && budget.getStartDate() != null && 
            budget.getEndDate().isBefore(budget.getStartDate())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        // Si no se proporciona startDate, usar fecha por defecto según el período
        LocalDate startDate = budget.getStartDate();
        if (startDate == null) {
            if ("monthly".equals(budget.getPeriod())) {
                startDate = YearMonth.now().atDay(1);
            } else {
                startDate = LocalDate.now().withDayOfYear(1);
            }
        }

        String sql = "INSERT INTO budgets (user_id, category_id, amount, period, start_date, end_date, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING budget_id";

        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                budget.getCategoryId(),
                budget.getAmount(),
                budget.getPeriod(),
                startDate,
                budget.getEndDate()
        );

        budget.setBudgetId(id);
        budget.setUserId(userId);
        budget.setStartDate(startDate);
        return budget;
    }

    @Override
    @Transactional
    public Budget updateBudget(Long userId, Long budgetId, Budget budget) {
        // Validar que el presupuesto existe y pertenece al usuario
        String checkSql = "SELECT budget_id FROM budgets WHERE budget_id = ? AND user_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, budgetId, userId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Presupuesto no encontrado o no pertenece al usuario");
        }

        // Construir UPDATE dinámicamente
        List<Object> params = new ArrayList<>();
        List<String> updates = new ArrayList<>();

        if (budget.getAmount() != null) {
            if (budget.getAmount() <= 0) {
                throw new IllegalArgumentException("El monto del presupuesto debe ser mayor a 0");
            }
            updates.add("amount = ?");
            params.add(budget.getAmount());
        }

        if (budget.getPeriod() != null) {
            if (!budget.getPeriod().equals("monthly") && !budget.getPeriod().equals("yearly")) {
                throw new IllegalArgumentException("El período debe ser 'monthly' o 'yearly'");
            }
            updates.add("period = ?");
            params.add(budget.getPeriod());
        }

        if (budget.getStartDate() != null) {
            updates.add("start_date = ?");
            params.add(budget.getStartDate());
        } else {
            // Permitir establecer start_date a NULL
            updates.add("start_date = NULL");
        }

        if (budget.getEndDate() != null) {
            updates.add("end_date = ?");
            params.add(budget.getEndDate());
        } else {
            // Permitir establecer end_date a NULL
            updates.add("end_date = NULL");
        }

        // Validar que end_date >= start_date si ambas están presentes
        if (budget.getEndDate() != null && budget.getStartDate() != null &&
            budget.getEndDate().isBefore(budget.getStartDate())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        if (updates.isEmpty()) {
            // No hay nada que actualizar
            return getBudget(userId, budgetId);
        }

        updates.add("updated_at = CURRENT_TIMESTAMP");
        params.add(userId);
        params.add(budgetId);

        String sql = "UPDATE budgets SET " + String.join(", ", updates) +
                " WHERE user_id = ? AND budget_id = ?";

        jdbcTemplate.update(sql, params.toArray());

        return getBudget(userId, budgetId);
    }

    @Override
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        String sql = "DELETE FROM budgets WHERE user_id = ? AND budget_id = ?";
        int deleted = jdbcTemplate.update(sql, userId, budgetId);
        
        if (deleted == 0) {
            throw new RuntimeException("Presupuesto no encontrado o no pertenece al usuario");
        }
    }

    private Budget getBudget(Long userId, Long budgetId) {
        String sql = "SELECT * FROM budgets WHERE user_id = ? AND budget_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, budgetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Presupuesto no encontrado");
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setBudgetId(rs.getLong("budget_id"));
        budget.setUserId(rs.getLong("user_id"));
        budget.setCategoryId(rs.getLong("category_id"));
        budget.setAmount(rs.getDouble("amount"));
        budget.setPeriod(rs.getString("period"));
        
        java.sql.Date startDate = rs.getDate("start_date");
        budget.setStartDate(startDate != null ? startDate.toLocalDate() : null);
        
        java.sql.Date endDate = rs.getDate("end_date");
        budget.setEndDate(endDate != null ? endDate.toLocalDate() : null);
        
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        budget.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        budget.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        
        return budget;
    }
}

