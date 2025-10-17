package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceUseCase implements DashboardServicePort {

    private final JdbcTemplate jdbcTemplate;

    public DashboardServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DashboardMetrics getMetrics(Long userId, LocalDate startDate, LocalDate endDate) {
        DashboardMetrics metrics = new DashboardMetrics();

        String sqlIncome = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id = ? AND transaction_type='income' AND transaction_date BETWEEN ? AND ?";
        String sqlExpense = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id = ? AND transaction_type='expense' AND transaction_date BETWEEN ? AND ?";

        BigDecimal income = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, startDate, endDate);
        BigDecimal expense = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, startDate, endDate);

        metrics.setTotalIncome(income);
        metrics.setTotalExpenses(expense);
        metrics.setNetBalance(income.subtract(expense));

        return metrics;
    }

    @Override
    public PeriodSummary getPeriodSummary(Long userId, String period) {
        LocalDate start;
        LocalDate end;

        if ("lastMonth".equalsIgnoreCase(period)) {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            start = lastMonth.atDay(1);
            end = lastMonth.atEndOfMonth();
        } else if ("year".equalsIgnoreCase(period)) {
            start = Year.now().atDay(1);
            end = Year.now().atMonth(12).atEndOfMonth();
        } else {
            YearMonth current = YearMonth.now();
            start = current.atDay(1);
            end = current.atEndOfMonth();
        }

        String sqlIncome = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND transaction_type='income' AND transaction_date BETWEEN ? AND ?";
        String sqlExpense = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND transaction_type='expense' AND transaction_date BETWEEN ? AND ?";

        BigDecimal income = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, start, end);
        BigDecimal expense = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, start, end);

        PeriodSummary summary = new PeriodSummary();
        summary.setPeriod(period);
        summary.setStartDate(start);
        summary.setEndDate(end);
        summary.setTotalIncome(income);
        summary.setTotalExpenses(expense);
        summary.setNetProfit(income.subtract(expense));

        return summary;
    }

    @Override
    public List<PeriodSummary> getMonthlySummary(Long userId, Integer year) {
        if (year == null) year = LocalDate.now().getYear();

        List<PeriodSummary> summaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            String sqlIncome = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND transaction_type='income' AND transaction_date BETWEEN ? AND ?";
            String sqlExpense = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND transaction_type='expense' AND transaction_date BETWEEN ? AND ?";

            BigDecimal income = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, start, end);
            BigDecimal expense = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, start, end);

            PeriodSummary summary = new PeriodSummary();
            summary.setPeriod(ym.toString());
            summary.setStartDate(start);
            summary.setEndDate(end);
            summary.setTotalIncome(income);
            summary.setTotalExpenses(expense);
            summary.setNetProfit(income.subtract(expense));

            summaries.add(summary);
        }

        return summaries;
    }

    @Override
    public AssetPerformance getAssetPerformance(Long userId, Long assetId, LocalDate startDate, LocalDate endDate) {
        AssetPerformance perf = new AssetPerformance();
        perf.setAssetId(assetId);

        String sqlInitial = "SELECT acquisition_value FROM assets WHERE user_id=? AND asset_id=?";
        BigDecimal initialValue = jdbcTemplate.queryForObject(sqlInitial, BigDecimal.class, userId, assetId);

        String sqlCurrent = "SELECT current_value FROM assets WHERE user_id=? AND asset_id=?";
        Double currentValue = jdbcTemplate.queryForObject(sqlCurrent, Double.class, userId, assetId);

        perf.setInitialValue(initialValue);
        perf.setCurrentValue(currentValue);
        if (initialValue.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal roi = BigDecimal.valueOf(currentValue)
                    .subtract(initialValue)
                    .divide(initialValue, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            perf.setRoi(roi);
        } else {
            perf.setRoi(BigDecimal.ZERO);
        }
        return perf;
    }

    @Override
    public LiabilityProgress getLiabilityProgress(Long userId, Long liabilityId) {
        LiabilityProgress progress = new LiabilityProgress();
        progress.setLiabilityId(liabilityId);

        // Obtener principal desde la tabla liabilities
        String sqlPrincipal = "SELECT principal_amount FROM liabilities WHERE user_id=? AND liability_id=?";
        BigDecimal principal = jdbcTemplate.queryForObject(sqlPrincipal, BigDecimal.class, userId, liabilityId);

        // Obtener el último outstanding_balance desde liability_values (última valoración)
        String sqlOutstanding = "SELECT outstanding_balance FROM liability_values WHERE liability_id=? ORDER BY valuation_date DESC LIMIT 1";
        BigDecimal outstanding = jdbcTemplate.query(sqlOutstanding, rs -> {
            if (rs.next()) {
                return rs.getBigDecimal("outstanding_balance");
            }
            return null;
        }, liabilityId);

        // Seguridad ante nulos
        BigDecimal remaining = outstanding != null ? outstanding : BigDecimal.ZERO;
        BigDecimal paid = (principal != null) ? principal.subtract(remaining) : BigDecimal.ZERO;

        progress.setPrincipalPaid(paid);
        progress.setRemainingBalance(remaining);

        return progress;
    }
}