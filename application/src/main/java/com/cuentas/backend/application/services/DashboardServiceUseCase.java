package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.DashboardServicePort;
import com.cuentas.backend.domain.AssetPerformance;
import com.cuentas.backend.domain.DashboardMetrics;
import com.cuentas.backend.domain.LiabilityProgress;
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

        String sqlIncome = "SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
                "WHERE t.user_id = ? AND t.transaction_date BETWEEN ? AND ? AND t.transaction_type = 'income'";
        BigDecimal totalIncome = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, startDate, endDate);
        metrics.setTotalIncome(totalIncome);

        String sqlExpense = "SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
                "WHERE t.user_id = ? AND t.transaction_date BETWEEN ? AND ? AND t.transaction_type = 'expense'";
        BigDecimal totalExpense = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, startDate, endDate);
        metrics.setTotalExpenses(totalExpense);

        metrics.setNetBalance(totalIncome.subtract(totalExpense));

        String sqlBestAsset = "SELECT asset_id, current_value FROM assets WHERE user_id=? ORDER BY current_value DESC LIMIT 1";
        jdbcTemplate.query(sqlBestAsset, rs -> {
            if (rs.next()) {
                var asset = new com.cuentas.backend.domain.Asset();
                asset.setAssetId(rs.getLong("asset_id"));
                asset.setCurrentValue(rs.getDouble("current_value"));
                metrics.setBestAsset(asset);
            }
        }, userId);

        String sqlWorstAsset = "SELECT asset_id, current_value FROM assets WHERE user_id=? ORDER BY current_value ASC LIMIT 1";
        jdbcTemplate.query(sqlWorstAsset, rs -> {
            if (rs.next()) {
                var asset = new com.cuentas.backend.domain.Asset();
                asset.setAssetId(rs.getLong("asset_id"));
                asset.setCurrentValue(rs.getDouble("current_value"));
                metrics.setWorstAsset(asset);
            }
        }, userId);

        return metrics;
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
            BigDecimal current = BigDecimal.valueOf(currentValue);
            BigDecimal roi = current.subtract(initialValue)
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

        String sql = "SELECT principal_amount, outstanding_balance FROM liabilities WHERE user_id=? AND liability_id=?";
        jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                BigDecimal principal = rs.getBigDecimal("principal_amount");
                BigDecimal remaining = rs.getBigDecimal("outstanding_balance");
                BigDecimal paid = principal.subtract(remaining);
                progress.setPrincipalPaid(paid);
                progress.setRemainingBalance(remaining);

                // Simulación de intereses pagados: sumatoria de transacciones relacionadas
                String sqlInterest = "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND liability_id=? AND amount>0";
                BigDecimal interestPaid = jdbcTemplate.queryForObject(sqlInterest, BigDecimal.class, userId, liabilityId);
                progress.setInterestPaid(interestPaid);

                BigDecimal progressPct = BigDecimal.ZERO;
                if (principal.compareTo(BigDecimal.ZERO) != 0) {
                    progressPct = paid.divide(principal, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
                }
                progress.setProgressPercentage(progressPct);
            }
        }, userId, liabilityId);

        return progress;
    }
}
