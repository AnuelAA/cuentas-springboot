package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.BudgetServicePort;
import com.cuentas.backend.domain.Budget;
import com.cuentas.backend.domain.BudgetStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceUseCase implements BudgetServicePort {

    private final JdbcTemplate jdbcTemplate;

    public BudgetServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Budget createBudget(Long userId, Budget budget) {
        String sql = "INSERT INTO budgets (user_id, category_id, amount, period, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?) RETURNING budget_id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                budget.getCategoryId(),
                budget.getAmount(),
                budget.getPeriod(),
                budget.getStartDate(),
                budget.getEndDate());
        budget.setBudgetId(id);
        budget.setUserId(userId);
        return budget;
    }

    @Override
    public Budget getBudget(Long userId, Long budgetId) {
        String sql = "SELECT * FROM budgets WHERE user_id = ? AND budget_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, budgetId);
    }

    @Override
    public List<Budget> listBudgets(Long userId) {
        String sql = "SELECT * FROM budgets WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    @Override
    public Budget updateBudget(Long userId, Long budgetId, Budget budget) {
        String sql = "UPDATE budgets SET category_id = ?, amount = ?, period = ?, start_date = ?, end_date = ?, updated_at = NOW() WHERE user_id = ? AND budget_id = ?";
        jdbcTemplate.update(sql,
                budget.getCategoryId(),
                budget.getAmount(),
                budget.getPeriod(),
                budget.getStartDate(),
                budget.getEndDate(),
                userId,
                budgetId);
        return getBudget(userId, budgetId);
    }

    @Override
    public void deleteBudget(Long userId, Long budgetId) {
        String sql = "DELETE FROM budgets WHERE user_id = ? AND budget_id = ?";
        jdbcTemplate.update(sql, userId, budgetId);
    }

    @Override
    public BudgetStatus getBudgetStatus(Long userId) {
        LocalDate now = LocalDate.now();
        
        // Obtener presupuestos activos (que no han expirado o no tienen fecha de fin)
        String sqlBudgets = "SELECT b.budget_id, b.category_id, c.name AS category_name, b.amount, b.period, b.start_date, b.end_date " +
                "FROM budgets b " +
                "JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? " +
                "AND (b.end_date IS NULL OR b.end_date >= ?) " +
                "AND (b.start_date IS NULL OR b.start_date <= ?) " +
                "ORDER BY c.name";
        
        List<Map<String, Object>> budgets = jdbcTemplate.queryForList(sqlBudgets, userId, now, now);
        
        List<BudgetStatus.BudgetStatusItem> items = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        
        for (Map<String, Object> budgetRow : budgets) {
            Long budgetId = ((Number) budgetRow.get("budget_id")).longValue();
            Long categoryId = ((Number) budgetRow.get("category_id")).longValue();
            String categoryName = (String) budgetRow.get("category_name");
            BigDecimal budgetAmount = (BigDecimal) budgetRow.get("amount");
            String period = (String) budgetRow.get("period");
            
            // Calcular gastos según el periodo
            BigDecimal spent = calculateSpent(userId, categoryId, period, now);
            BigDecimal remaining = budgetAmount.subtract(spent);
            Double percentageUsed = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0.0;
            
            items.add(BudgetStatus.BudgetStatusItem.builder()
                    .budgetId(budgetId)
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .budgetAmount(budgetAmount)
                    .spent(spent)
                    .remaining(remaining)
                    .percentageUsed(percentageUsed)
                    .period(period)
                    .build());
            
            totalBudget = totalBudget.add(budgetAmount);
            totalSpent = totalSpent.add(spent);
        }
        
        BigDecimal totalRemaining = totalBudget.subtract(totalSpent);
        
        return BudgetStatus.builder()
                .items(items)
                .totalBudget(totalBudget)
                .totalSpent(totalSpent)
                .totalRemaining(totalRemaining)
                .build();
    }
    
    private BigDecimal calculateSpent(Long userId, Long categoryId, String period, LocalDate now) {
        String sql;
        LocalDate startDate;
        
        if ("monthly".equals(period)) {
            startDate = now.withDayOfMonth(1);
            sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                    "WHERE user_id = ? AND category_id = ? AND transaction_type = 'expense' " +
                    "AND transaction_date >= ? AND transaction_date <= ?";
        } else { // yearly
            startDate = now.withDayOfYear(1);
            sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                    "WHERE user_id = ? AND category_id = ? AND transaction_type = 'expense' " +
                    "AND transaction_date >= ? AND transaction_date <= ?";
        }
        
        try {
            BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, categoryId, startDate, now);
            return result != null ? result : BigDecimal.ZERO;
        } catch (DataAccessException e) {
            return BigDecimal.ZERO;
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setBudgetId(rs.getLong("budget_id"));
        b.setUserId(rs.getLong("user_id"));
        b.setCategoryId(rs.getLong("category_id"));
        b.setAmount(rs.getBigDecimal("amount"));
        b.setPeriod(rs.getString("period"));
        b.setStartDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null);
        b.setEndDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null);
        b.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        b.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return b;
    }
}

