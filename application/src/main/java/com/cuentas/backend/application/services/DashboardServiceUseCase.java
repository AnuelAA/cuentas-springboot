package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.DashboardMetrics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DashboardServiceUseCase implements DashboardServicePort {

    private final JdbcTemplate jdbcTemplate;

    public DashboardServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DashboardMetrics getMetrics(Long userId, LocalDate startDate, LocalDate endDate) {
        DashboardMetrics metrics = new DashboardMetrics();

        // Total ingresos
        String sqlIncome = "SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
                "WHERE t.user_id = ? AND t.transaction_date BETWEEN ? AND ? AND t.transaction_type = 'income'";
        BigDecimal totalIncome = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, startDate, endDate);
        metrics.setTotalIncome(totalIncome);

        // Total gastos
        String sqlExpense = "SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
                "WHERE t.user_id = ? AND t.transaction_date BETWEEN ? AND ? AND t.transaction_type = 'expense'";
        BigDecimal totalExpense = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, startDate, endDate);
        metrics.setTotalExpense(totalExpense);

        // Beneficio neto
        metrics.setNetProfit(totalIncome.subtract(totalExpense));

        return metrics;
    }
}
