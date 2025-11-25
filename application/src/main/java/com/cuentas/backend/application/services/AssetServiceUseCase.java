package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.AssetServicePort;
import com.cuentas.backend.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import java.math.BigDecimal;
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
    private static final String SQL_SELECT_ASSET_VALUES_BY_ASSET =
            "SELECT value_id, asset_id, valuation_date, current_value, created_at " +
                    "FROM asset_values WHERE asset_id = ? ORDER BY valuation_date";

    private static final String SQL_SELECT_ASSET_VALUES_BY_USER =
            "SELECT av.value_id, av.asset_id, av.valuation_date, av.current_value, av.created_at " +
                    "FROM asset_values av JOIN assets a ON av.asset_id = a.asset_id " +
                    "WHERE a.user_id = ? ORDER BY av.asset_id, av.valuation_date";
    public AssetServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Asset createAsset(Long userId, Asset asset) {
        // Si se está marcando como principal, primero desmarcar cualquier otro activo principal del usuario
        if (asset.getIsPrimary() != null && asset.getIsPrimary()) {
            String unsetPrimarySql = "UPDATE assets SET is_primary = FALSE WHERE user_id = ? AND is_primary = TRUE";
            jdbcTemplate.update(unsetPrimarySql, userId);
        }

        String sql = "INSERT INTO assets (user_id, asset_type_id, name, description, acquisition_date, acquisition_value, ownership_percentage, is_primary) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING asset_id";

        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                asset.getAssetTypeId(),
                asset.getName(),
                asset.getDescription(),
                asset.getAcquisitionDate(),
                asset.getAcquisitionValue(),
                asset.getOwnershipPercentage() != null ? asset.getOwnershipPercentage() : 100.00,
                asset.getIsPrimary() != null ? asset.getIsPrimary() : false
        );

        asset.setAssetId(id);
        asset.setUserId(userId);
        return asset;
    }

    @Override
    public Asset getAsset(Long userId, Long assetId) {
        String sql = "SELECT * FROM assets WHERE user_id = ? AND asset_id = ?";
        Asset asset = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, assetId);

        List<AssetValue> values = jdbcTemplate.query(SQL_SELECT_ASSET_VALUES_BY_ASSET,
                (rs, rowNum) -> mapAssetValue(rs),
                assetId);

        asset.setAssetValues(values);
        return asset;
    }

    @Override
    public List<Asset> listAssets(Long userId) {
        String sql = "SELECT * FROM assets WHERE user_id = ?";
        List<Asset> assets = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);

        if (assets.isEmpty()) return assets;

        // Map asset_id -> list of AssetValue
        Map<Long, List<AssetValue>> valuesMap = new HashMap<>();
        jdbcTemplate.query(SQL_SELECT_ASSET_VALUES_BY_USER, rs -> {
            AssetValue av = mapAssetValue(rs);
            valuesMap.computeIfAbsent(av.getAssetId(), k -> new ArrayList<>()).add(av);
        }, userId);

        for (Asset asset : assets) {
            List<AssetValue> vals = valuesMap.getOrDefault(asset.getAssetId(), new ArrayList<>());
            asset.setAssetValues(vals);
        }

        return assets;
    }

    @Override
    @Transactional
    public Asset updateAsset(Long userId, Long assetId, Asset asset) {
        // Si se está marcando como principal, primero desmarcar cualquier otro activo principal del usuario
        if (asset.getIsPrimary() != null && asset.getIsPrimary()) {
            String unsetPrimarySql = "UPDATE assets SET is_primary = FALSE WHERE user_id = ? AND is_primary = TRUE AND asset_id != ?";
            jdbcTemplate.update(unsetPrimarySql, userId, assetId);
        }

        String sql = "UPDATE assets SET asset_type_id = ?, name = ?, description = ?, acquisition_date = ?, acquisition_value = ?, ownership_percentage = ?, is_primary = ?, updated_at = NOW() " +
                "WHERE user_id = ? AND asset_id = ?";
        jdbcTemplate.update(sql,
                asset.getAssetTypeId(),
                asset.getName(),
                asset.getDescription(),
                asset.getAcquisitionDate(),
                asset.getAcquisitionValue(),
                asset.getOwnershipPercentage(),
                asset.getIsPrimary() != null ? asset.getIsPrimary() : false,
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

    @Override
    @Transactional
    public AssetValue upsertAssetValue(Long userId, Long assetId, LocalDate valuationDate, Double currentValue, Double acquisitionValue) {
        // Validar que el asset existe y pertenece al usuario (sin cargar valores para evitar overhead)
        String checkSql = "SELECT asset_id FROM assets WHERE user_id = ? AND asset_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, userId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Asset no encontrado o no pertenece al usuario");
        }

        // Validar que currentValue es positivo
        if (currentValue == null || currentValue <= 0) {
            throw new IllegalArgumentException("currentValue debe ser mayor a 0");
        }

        // Validar que acquisitionValue es no negativo si se proporciona
        if (acquisitionValue != null && acquisitionValue < 0) {
            throw new IllegalArgumentException("acquisitionValue debe ser mayor o igual a 0");
        }

        // Buscar si ya existe una valoración para esta fecha
        String selectSql = "SELECT value_id FROM asset_values WHERE asset_id = ? AND valuation_date = ?";
        Long existingValueId = null;
        try {
            existingValueId = jdbcTemplate.queryForObject(selectSql, Long.class, assetId, valuationDate);
        } catch (Exception e) {
            // No existe, continuar para crear
        }

        if (existingValueId != null) {
            // Actualizar existente
            // Nota: acquisitionValue no está en la tabla según el schema, pero lo dejamos preparado
            String updateSql = "UPDATE asset_values SET current_value = ? WHERE value_id = ?";
            jdbcTemplate.update(updateSql, currentValue, existingValueId);
            
            AssetValue updated = new AssetValue();
            updated.setAssetValueId(existingValueId);
            updated.setAssetId(assetId);
            updated.setValuationDate(valuationDate);
            updated.setCurrentValue(currentValue);
            updated.setAcquisitionValue(acquisitionValue); // Guardado en memoria aunque no en BD
            return updated;
        } else {
            // Insertar nuevo
            String insertSql = "INSERT INTO asset_values (asset_id, valuation_date, current_value) VALUES (?, ?, ?) RETURNING value_id";
            Long valueId = jdbcTemplate.queryForObject(insertSql, Long.class, assetId, valuationDate, currentValue);
            
            AssetValue created = new AssetValue();
            created.setAssetValueId(valueId);
            created.setAssetId(assetId);
            created.setValuationDate(valuationDate);
            created.setCurrentValue(currentValue);
            created.setAcquisitionValue(acquisitionValue); // Guardado en memoria aunque no en BD
            return created;
        }
    }

    @Override
    @Transactional
    public AssetValue updateAssetValue(Long userId, Long assetId, Long valuationId, LocalDate valuationDate, Double currentValue, Double acquisitionValue) {
        // Validar que el asset existe y pertenece al usuario
        String checkAssetSql = "SELECT asset_id FROM assets WHERE user_id = ? AND asset_id = ?";
        try {
            jdbcTemplate.queryForObject(checkAssetSql, Long.class, userId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Asset no encontrado o no pertenece al usuario");
        }

        // Validar que la valoración existe y pertenece al asset
        String checkValuationSql = "SELECT value_id FROM asset_values WHERE value_id = ? AND asset_id = ?";
        try {
            jdbcTemplate.queryForObject(checkValuationSql, Long.class, valuationId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Valoración no encontrada o no pertenece al activo");
        }

        // Validar que currentValue es positivo
        if (currentValue == null || currentValue <= 0) {
            throw new IllegalArgumentException("currentValue debe ser mayor a 0");
        }

        // Validar que acquisitionValue es no negativo si se proporciona
        if (acquisitionValue != null && acquisitionValue < 0) {
            throw new IllegalArgumentException("acquisitionValue debe ser mayor o igual a 0");
        }

        // Validar que valuationDate no es null
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate es requerido");
        }

        // Actualizar la valoración
        String updateSql = "UPDATE asset_values SET valuation_date = ?, current_value = ? WHERE value_id = ?";
        int updated = jdbcTemplate.update(updateSql, valuationDate, currentValue, valuationId);
        
        if (updated == 0) {
            throw new RuntimeException("No se pudo actualizar la valoración");
        }

        AssetValue updatedValue = new AssetValue();
        updatedValue.setAssetValueId(valuationId);
        updatedValue.setAssetId(assetId);
        updatedValue.setValuationDate(valuationDate);
        updatedValue.setCurrentValue(currentValue);
        updatedValue.setAcquisitionValue(acquisitionValue);
        return updatedValue;
    }

    @Override
    @Transactional
    public void deleteAssetValue(Long userId, Long assetId, Long valuationId) {
        // Validar que el asset existe y pertenece al usuario
        String checkAssetSql = "SELECT asset_id FROM assets WHERE user_id = ? AND asset_id = ?";
        try {
            jdbcTemplate.queryForObject(checkAssetSql, Long.class, userId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Asset no encontrado o no pertenece al usuario");
        }

        // Validar que la valoración existe y pertenece al asset
        String checkValuationSql = "SELECT value_id FROM asset_values WHERE value_id = ? AND asset_id = ?";
        try {
            jdbcTemplate.queryForObject(checkValuationSql, Long.class, valuationId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Valoración no encontrada o no pertenece al activo");
        }

        // Eliminar la valoración
        String deleteSql = "DELETE FROM asset_values WHERE value_id = ?";
        int deleted = jdbcTemplate.update(deleteSql, valuationId);
        
        if (deleted == 0) {
            throw new RuntimeException("No se pudo eliminar la valoración");
        }
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

    @Override
    public AssetDetail getAssetDetail(Long userId, Long assetId) {
        // Obtener el activo
        Asset asset;
        try {
            asset = getAsset(userId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Activo no encontrado o no pertenece al usuario");
        }

        // Obtener el valor actual (último registrado)
        String sqlCurrentValue = "SELECT current_value FROM asset_values WHERE asset_id = ? ORDER BY valuation_date DESC LIMIT 1";
        BigDecimal currentValue = jdbcTemplate.query(sqlCurrentValue, rs -> {
            if (rs.next()) {
                return BigDecimal.valueOf(rs.getDouble("current_value"));
            }
            return BigDecimal.ZERO;
        }, assetId);

        // Calcular ingresos y gastos del activo (related_asset_id)
        String sqlIncome = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND related_asset_id = ? AND transaction_type = 'income'";
        String sqlExpense = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND related_asset_id = ? AND transaction_type = 'expense'";
        
        BigDecimal totalIncome = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, assetId);
        BigDecimal totalExpenses = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, assetId);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        // Calcular ROI
        double invested = asset.getAcquisitionValue() != null ? asset.getAcquisitionValue() : 0.0;
        double roiPercentage = invested != 0 ? (netProfit.doubleValue() / invested) * 100 : 0.0;

        // Contar transacciones
        String sqlCount = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND related_asset_id = ?";
        Integer transactionCount = jdbcTemplate.queryForObject(sqlCount, Integer.class, userId, assetId);

        // Obtener TODAS las transacciones (sin límite)
        String sqlRecent = "SELECT * FROM transactions WHERE user_id = ? AND related_asset_id = ? ORDER BY transaction_date DESC, transaction_id DESC";
        List<Transaction> recentTransactions = jdbcTemplate.query(sqlRecent, (rs, rowNum) -> mapTransaction(rs), userId, assetId);

        // Obtener TODO el historial de valores (sin límite de tiempo)
        String sqlValueHistory = "SELECT * FROM asset_values WHERE asset_id = ? ORDER BY valuation_date DESC";
        List<AssetValue> valueHistory = jdbcTemplate.query(sqlValueHistory, (rs, rowNum) -> mapAssetValue(rs), assetId);

        return AssetDetail.builder()
                .asset(asset)
                .currentValue(currentValue)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .roiPercentage(roiPercentage)
                .transactionCount(transactionCount)
                .recentTransactions(recentTransactions)
                .valueHistory(valueHistory)
                .build();
    }

    @Override
    public Asset getPrimaryAsset(Long userId) {
        String sql = "SELECT * FROM assets WHERE user_id = ? AND is_primary = TRUE LIMIT 1";
        try {
            Asset asset = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId);
            
            // Cargar valores del activo
            List<AssetValue> values = jdbcTemplate.query(SQL_SELECT_ASSET_VALUES_BY_ASSET,
                    (rs, rowNum) -> mapAssetValue(rs),
                    asset.getAssetId());
            asset.setAssetValues(values);
            
            return asset;
        } catch (DataAccessException e) {
            return null; // No hay activo principal
        }
    }

    @Override
    @Transactional
    public Asset setPrimaryAsset(Long userId, Long assetId) {
        // Verificar que el activo existe y pertenece al usuario
        String checkSql = "SELECT asset_id FROM assets WHERE user_id = ? AND asset_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, userId, assetId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Activo no encontrado o no pertenece al usuario");
        }

        // Desmarcar cualquier otro activo principal del usuario
        String unsetPrimarySql = "UPDATE assets SET is_primary = FALSE WHERE user_id = ? AND is_primary = TRUE";
        jdbcTemplate.update(unsetPrimarySql, userId);

        // Marcar el activo especificado como principal
        String setPrimarySql = "UPDATE assets SET is_primary = TRUE, updated_at = NOW() WHERE user_id = ? AND asset_id = ?";
        jdbcTemplate.update(setPrimarySql, userId, assetId);

        // Retornar el activo actualizado
        return getAsset(userId, assetId);
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("transaction_id"));
        t.setUserId(rs.getLong("user_id"));
        Long categoryId = rs.getLong("category_id");
        t.setCategoryId(rs.wasNull() ? null : categoryId);
        Long assetId = rs.getLong("asset_id");
        t.setAssetId(rs.wasNull() ? null : assetId);
        Long liabilityId = rs.getLong("liability_id");
        t.setLiabilityId(rs.wasNull() ? null : liabilityId);
        Long relatedAssetId = rs.getLong("related_asset_id");
        t.setRelatedAssetId(rs.wasNull() ? null : relatedAssetId);
        t.setType(rs.getString("transaction_type"));
        t.setAmount(rs.getDouble("amount"));
        t.setTransactionDate(rs.getDate("transaction_date") != null ? rs.getDate("transaction_date").toLocalDate() : null);
        t.setDescription(rs.getString("description"));
        return t;
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
        asset.setOwnershipPercentage(rs.getDouble("ownership_percentage"));
        asset.setIsPrimary(rs.getBoolean("is_primary"));
        return asset;
    }
    private AssetValue mapAssetValue(java.sql.ResultSet rs) throws java.sql.SQLException {
        AssetValue av = new AssetValue();
        av.setAssetValueId(rs.getLong("value_id"));
        av.setAssetId(rs.getLong("asset_id"));
        java.sql.Date vd = rs.getDate("valuation_date");
        av.setValuationDate(vd != null ? vd.toLocalDate() : null);
        av.setCurrentValue(rs.getDouble("current_value"));
        return av;
    }
}
