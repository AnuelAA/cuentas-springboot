package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.AssetServicePort;
import com.cuentas.backend.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetServiceUseCase implements AssetServicePort {

    private final JdbcTemplate jdbcTemplate;

    public AssetServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Asset createAsset(Long userId, Asset asset) {
        String sql = "INSERT INTO assets (user_id, asset_type_id, name, description, acquisition_date, acquisition_value, current_value) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING asset_id";

        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                asset.getAssetTypeId(),
                asset.getName(),
                asset.getDescription(),
                asset.getAcquisitionDate(),
                asset.getAcquisitionValue(),
                asset.getCurrentValue()
        );

        asset.setAssetId(id);
        asset.setUserId(userId);
        return asset;
    }

    @Override
    public Asset getAsset(Long userId, Long assetId) {
        String sql = "SELECT * FROM assets WHERE user_id = ? AND asset_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, assetId);
    }

    @Override
    public List<Asset> listAssets(Long userId) {
        String sql = "SELECT * FROM assets WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    @Override
    public Asset updateAsset(Long userId, Long assetId, Asset asset) {
        String sql = "UPDATE assets SET asset_type_id = ?, name = ?, description = ?, acquisition_date = ?, acquisition_value = ?, current_value = ?, updated_at = NOW() " +
                "WHERE user_id = ? AND asset_id = ?";
        jdbcTemplate.update(sql,
                asset.getAssetTypeId(),
                asset.getName(),
                asset.getDescription(),
                asset.getAcquisitionDate(),
                asset.getAcquisitionValue(),
                asset.getCurrentValue(),
                userId,
                assetId
        );
        return getAsset(userId, assetId);
    }

    @Override
    public void deleteAsset(Long userId, Long assetId) {
        String sql = "DELETE FROM assets WHERE user_id = ? AND asset_id = ?";
        jdbcTemplate.update(sql, userId, assetId);
    }

    // ===============================
    // NUEVAS FUNCIONES ROI
    // ===============================

    @Override
    public AssetROI calculateAssetROI(Long userId, Long assetId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT transaction_type, SUM(amount) AS total " +
                        "FROM transactions WHERE user_id = ? AND related_asset_id = ?"
        );
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(assetId);

        if (startDate != null) {
            sql.append(" AND transaction_date >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND transaction_date <= ?");
            params.add(endDate);
        }
        sql.append(" GROUP BY transaction_type");

        Map<String, Double> totals = jdbcTemplate.query(sql.toString(), rs -> {
            Map<String, Double> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("transaction_type"), rs.getDouble("total"));
            }
            return map;
        }, params.toArray());

        double totalIncome = totals.getOrDefault("income", 0.0);
        double totalExpenses = totals.getOrDefault("expense", 0.0);
        double netProfit = totalIncome - totalExpenses;

        Asset asset = getAsset(userId, assetId);
        double invested = asset.getAcquisitionValue() != null ? asset.getAcquisitionValue() : 0.0;
        double roiPercentage = invested != 0 ? (netProfit / invested) * 100 : 0.0;

        return new AssetROI(assetId, totalIncome, totalExpenses, netProfit, roiPercentage);
    }

    @Override
    public List<MonthlyROI> calculateMonthlyROI(Long userId, Long assetId, Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        String sql = "SELECT DATE_TRUNC('month', transaction_date) AS month, " +
                "SUM(CASE WHEN transaction_type = 'income' THEN amount ELSE 0 END) AS income, " +
                "SUM(CASE WHEN transaction_type = 'expense' THEN amount ELSE 0 END) AS expenses " +
                "FROM transactions " +
                "WHERE user_id = ? AND related_asset_id = ? AND EXTRACT(YEAR FROM transaction_date) = ? " +
                "GROUP BY month ORDER BY month";

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(assetId);
        params.add(targetYear);

        // Map de YearMonth -> [income, expenses]
        Map<YearMonth, double[]> monthSums = jdbcTemplate.query(sql, rs -> {
            Map<YearMonth, double[]> map = new HashMap<>();
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("month");
                if (ts == null) continue;
                YearMonth ym = YearMonth.from(ts.toLocalDateTime().toLocalDate());
                double income = rs.getDouble("income");
                double expenses = rs.getDouble("expenses");
                map.put(ym, new double[]{income, expenses});
            }
            return map;
        }, params.toArray());

        Asset asset = getAsset(userId, assetId);
        double invested = asset.getAcquisitionValue() != null ? asset.getAcquisitionValue() : 0.0;

        List<MonthlyROI> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(targetYear, m);
            double[] sums = monthSums.getOrDefault(ym, new double[]{0.0, 0.0});
            double income = sums[0];
            double expenses = sums[1];
            double netProfit = income - expenses;
            double roi = invested != 0 ? (netProfit / invested) * 100 : 0.0;
            result.add(new MonthlyROI(ym.toString(), income, expenses, netProfit, roi));
        }

        return result;
    }

    private Asset mapRow(ResultSet rs) throws SQLException {
        Asset asset = new Asset();
        asset.setAssetId(rs.getLong("asset_id"));
        asset.setUserId(rs.getLong("user_id"));
        asset.setAssetTypeId(rs.getLong("asset_type_id"));
        asset.setName(rs.getString("name"));
        asset.setDescription(rs.getString("description"));
        asset.setAcquisitionDate(rs.getDate("acquisition_date") != null ? rs.getDate("acquisition_date").toLocalDate() : null);
        asset.setAcquisitionValue(rs.getDouble("acquisition_value"));
        asset.setCurrentValue(rs.getDouble("current_value"));
        return asset;
    }
}
