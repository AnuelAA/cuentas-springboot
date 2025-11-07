package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.LiabilityServicePort;
import com.cuentas.backend.domain.Interest;
import com.cuentas.backend.domain.Liability;
import com.cuentas.backend.domain.LiabilityDetail;
import com.cuentas.backend.domain.LiabilityValue;
import com.cuentas.backend.domain.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class LiabilityServiceUseCase implements LiabilityServicePort {

    private final JdbcTemplate jdbcTemplate;

    // SQL para liability_values
    private static final String SQL_SELECT_LIABILITY_VALUES_BY_LIABILITY =
            "SELECT value_id, liability_id, valuation_date, end_date, outstanding_balance, created_at " +
                    "FROM liability_values WHERE liability_id = ? ORDER BY valuation_date";

    private static final String SQL_SELECT_LIABILITY_VALUES_BY_USER =
            "SELECT lv.value_id, lv.liability_id, lv.valuation_date, lv.end_date, lv.outstanding_balance, lv.created_at " +
                    "FROM liability_values lv JOIN liabilities l ON lv.liability_id = l.liability_id " +
                    "WHERE l.user_id = ? ORDER BY lv.liability_id, lv.valuation_date";

    // SQL para interests
    private static final String SQL_SELECT_INTERESTS_BY_LIABILITY =
            "SELECT interest_id, liability_id, type, annual_rate, start_date, created_at " +
                    "FROM interests WHERE liability_id = ? ORDER BY start_date";

    public LiabilityServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Liability createLiability(Long userId, Liability liability) {
        String sql = "INSERT INTO liabilities (user_id, liability_type_id, name, description, principal_amount, start_date) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING liability_id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                liability.getLiabilityTypeId(),
                liability.getName(),
                liability.getDescription(),
                liability.getPrincipalAmount(),
                liability.getStartDate()
        );
        liability.setLiabilityId(id);
        liability.setUserId(userId);
        return liability;
    }

    @Override
    public Liability getLiability(Long userId, Long liabilityId) {
        String sql = "SELECT * FROM liabilities WHERE user_id = ? AND liability_id = ?";
        Liability liability = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, liabilityId);

        // cargar valores asociados
        List<LiabilityValue> values = jdbcTemplate.query(SQL_SELECT_LIABILITY_VALUES_BY_LIABILITY,
                (rs, rowNum) -> mapLiabilityValue(rs),
                liabilityId);
        liability.setLiabilityValues(values);
        return liability;
    }

    @Override
    public List<Liability> listLiabilities(Long userId) {
        String sql = "SELECT * FROM liabilities WHERE user_id = ?";
        List<Liability> liabilities = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);

        if (liabilities.isEmpty()) return liabilities;

        // Map liability_id -> list of LiabilityValue
        Map<Long, List<LiabilityValue>> valuesMap = new HashMap<>();
        jdbcTemplate.query(SQL_SELECT_LIABILITY_VALUES_BY_USER, rs -> {
            LiabilityValue lv = mapLiabilityValue(rs);
            valuesMap.computeIfAbsent(lv.getLiabilityId(), k -> new ArrayList<>()).add(lv);
        }, userId);

        for (Liability l : liabilities) {
            List<LiabilityValue> vals = valuesMap.getOrDefault(l.getLiabilityId(), new ArrayList<>());
            l.setLiabilityValues(vals);
        }

        return liabilities;
    }

    @Override
    public Liability updateLiability(Long userId, Long liabilityId, Liability liability) {
        // Construir UPDATE dinámico solo con campos no nulos (actualización parcial)
        StringBuilder sql = new StringBuilder("UPDATE liabilities SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();
        
        if (liability.getName() != null) {
            sql.append(", name = ?");
            params.add(liability.getName());
        }
        if (liability.getDescription() != null) {
            sql.append(", description = ?");
            params.add(liability.getDescription());
        }
        if (liability.getLiabilityTypeId() != null) {
            sql.append(", liability_type_id = ?");
            params.add(liability.getLiabilityTypeId());
        }
        if (liability.getPrincipalAmount() != null) {
            sql.append(", principal_amount = ?");
            params.add(liability.getPrincipalAmount());
        }
        if (liability.getStartDate() != null) {
            sql.append(", start_date = ?");
            params.add(liability.getStartDate());
        }
        
        sql.append(" WHERE user_id = ? AND liability_id = ?");
        params.add(userId);
        params.add(liabilityId);
        
        jdbcTemplate.update(sql.toString(), params.toArray());
        return getLiability(userId, liabilityId);
    }

    @Override
    public void deleteLiability(Long userId, Long liabilityId) {
        String sql = "DELETE FROM liabilities WHERE user_id = ? AND liability_id = ?";
        jdbcTemplate.update(sql, userId, liabilityId);
    }

    @Override
    @Transactional
    public LiabilityValue upsertLiabilityValue(Long userId, Long liabilityId, LocalDate valuationDate, Double outstandingBalance, LocalDate endDate) {
        // Validar que el liability existe y pertenece al usuario (sin cargar valores para evitar overhead)
        String checkSql = "SELECT liability_id FROM liabilities WHERE user_id = ? AND liability_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, userId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Liability no encontrado o no pertenece al usuario");
        }

        // Validar que outstandingBalance es no negativo
        if (outstandingBalance == null || outstandingBalance < 0) {
            throw new IllegalArgumentException("outstandingBalance debe ser mayor o igual a 0");
        }

        // Validar que endDate es posterior o igual a valuationDate si se proporciona
        if (endDate != null && endDate.isBefore(valuationDate)) {
            throw new IllegalArgumentException("endDate debe ser posterior o igual a valuationDate");
        }

        // Buscar si ya existe un snapshot para esta fecha
        String selectSql = "SELECT value_id FROM liability_values WHERE liability_id = ? AND valuation_date = ?";
        Long existingValueId = null;
        try {
            existingValueId = jdbcTemplate.queryForObject(selectSql, Long.class, liabilityId, valuationDate);
        } catch (Exception e) {
            // No existe, continuar para crear
        }

        if (existingValueId != null) {
            // Actualizar existente
            String updateSql = "UPDATE liability_values SET outstanding_balance = ?, end_date = ? WHERE value_id = ?";
            jdbcTemplate.update(updateSql, outstandingBalance, endDate, existingValueId);
            
            LiabilityValue updated = new LiabilityValue();
            updated.setLiabilityValueId(existingValueId);
            updated.setLiabilityId(liabilityId);
            updated.setValuationDate(valuationDate);
            updated.setOutstandingBalance(outstandingBalance);
            updated.setEndDate(endDate);
            return updated;
        } else {
            // Insertar nuevo
            String insertSql = "INSERT INTO liability_values (liability_id, valuation_date, end_date, outstanding_balance) VALUES (?, ?, ?, ?) RETURNING value_id";
            Long valueId = jdbcTemplate.queryForObject(insertSql, Long.class, liabilityId, valuationDate, endDate, outstandingBalance);
            
            LiabilityValue created = new LiabilityValue();
            created.setLiabilityValueId(valueId);
            created.setLiabilityId(liabilityId);
            created.setValuationDate(valuationDate);
            created.setOutstandingBalance(outstandingBalance);
            created.setEndDate(endDate);
            return created;
        }
    }

    @Override
    @Transactional
    public Interest createInterest(Long userId, Long liabilityId, String type, Double annualRate, LocalDate startDate) {
        // Validar que el liability existe y pertenece al usuario
        String checkSql = "SELECT liability_id FROM liabilities WHERE user_id = ? AND liability_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, userId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Liability no encontrado o no pertenece al usuario");
        }

        // Validar tipo de interés
        if (type == null || type.trim().isEmpty()) {
            type = "fixed"; // Default
        } else {
            type = type.toLowerCase();
            if (!type.equals("fixed") && !type.equals("variable") && !type.equals("general")) {
                throw new IllegalArgumentException("Tipo de interés debe ser: 'fixed', 'variable' o 'general'");
            }
        }

        // Validar fecha de inicio
        if (startDate == null) {
            throw new IllegalArgumentException("startDate es obligatorio");
        }

        // Validar que annualRate es no negativo si se proporciona
        if (annualRate != null && annualRate < 0) {
            throw new IllegalArgumentException("annualRate debe ser mayor o igual a 0");
        }

        // Insertar nuevo interés
        String insertSql = "INSERT INTO interests (liability_id, type, annual_rate, start_date, created_at) " +
                "VALUES (?, ?, ?, ?, NOW()) RETURNING interest_id";
        Long interestId = jdbcTemplate.queryForObject(insertSql, Long.class, 
                liabilityId, type, annualRate, startDate);

        Interest interest = new Interest();
        interest.setInterestId(interestId);
        interest.setLiabilityId(liabilityId);
        interest.setType(type);
        interest.setAnnualRate(annualRate);
        interest.setStartDate(startDate);

        return interest;
    }

    @Override
    @Transactional
    public Interest updateInterest(Long userId, Long liabilityId, Long interestId, String type, Double annualRate, LocalDate startDate) {
        // Validar que el liability existe y pertenece al usuario
        String checkLiabilitySql = "SELECT liability_id FROM liabilities WHERE user_id = ? AND liability_id = ?";
        try {
            jdbcTemplate.queryForObject(checkLiabilitySql, Long.class, userId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Liability no encontrado o no pertenece al usuario");
        }

        // Validar que el interés existe y pertenece al pasivo correcto
        String checkInterestSql = "SELECT interest_id FROM interests WHERE interest_id = ? AND liability_id = ?";
        try {
            jdbcTemplate.queryForObject(checkInterestSql, Long.class, interestId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Interés no encontrado o no pertenece al pasivo indicado");
        }

        // Validar tipo de interés
        if (type == null || type.trim().isEmpty()) {
            type = "fixed"; // Default
        } else {
            type = type.toLowerCase();
            if (!type.equals("fixed") && !type.equals("variable") && !type.equals("general")) {
                throw new IllegalArgumentException("Tipo de interés debe ser: 'fixed', 'variable' o 'general'");
            }
        }

        // Validar fecha de inicio
        if (startDate == null) {
            throw new IllegalArgumentException("startDate es obligatorio");
        }

        // Validar que annualRate es no negativo si se proporciona
        if (annualRate != null && annualRate < 0) {
            throw new IllegalArgumentException("annualRate debe ser mayor o igual a 0");
        }

        // Actualizar el interés
        String updateSql = "UPDATE interests SET type = ?, annual_rate = ?, start_date = ? WHERE interest_id = ?";
        jdbcTemplate.update(updateSql, type, annualRate, startDate, interestId);

        Interest interest = new Interest();
        interest.setInterestId(interestId);
        interest.setLiabilityId(liabilityId);
        interest.setType(type);
        interest.setAnnualRate(annualRate);
        interest.setStartDate(startDate);

        return interest;
    }

    @Override
    @Transactional
    public void deleteInterest(Long userId, Long liabilityId, Long interestId) {
        // Validar que el liability existe y pertenece al usuario
        String checkLiabilitySql = "SELECT liability_id FROM liabilities WHERE user_id = ? AND liability_id = ?";
        try {
            jdbcTemplate.queryForObject(checkLiabilitySql, Long.class, userId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Liability no encontrado o no pertenece al usuario");
        }

        // Eliminar el interés
        String deleteSql = "DELETE FROM interests WHERE interest_id = ? AND liability_id = ?";
        int rowsAffected = jdbcTemplate.update(deleteSql, interestId, liabilityId);
        
        if (rowsAffected == 0) {
            throw new RuntimeException("Interés no encontrado o no pertenece al pasivo indicado");
        }
    }

    @Override
    public List<Interest> getInterests(Long userId, Long liabilityId) {
        // Validar que el liability existe y pertenece al usuario
        String checkSql = "SELECT liability_id FROM liabilities WHERE user_id = ? AND liability_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, userId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Liability no encontrado o no pertenece al usuario");
        }

        // Obtener todos los intereses del pasivo
        return jdbcTemplate.query(SQL_SELECT_INTERESTS_BY_LIABILITY,
                (rs, rowNum) -> mapInterest(rs),
                liabilityId);
    }

    private Interest mapInterest(ResultSet rs) throws SQLException {
        Interest interest = new Interest();
        interest.setInterestId(rs.getLong("interest_id"));
        interest.setLiabilityId(rs.getLong("liability_id"));
        interest.setType(rs.getString("type"));
        interest.setAnnualRate(rs.getDouble("annual_rate"));
        java.sql.Date sd = rs.getDate("start_date");
        interest.setStartDate(sd != null ? sd.toLocalDate() : null);
        interest.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
        return interest;
    }

    @Override
    public LiabilityDetail getLiabilityDetail(Long userId, Long liabilityId) {
        // Obtener el pasivo
        Liability liability;
        try {
            liability = getLiability(userId, liabilityId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Pasivo no encontrado o no pertenece al usuario");
        }

        // Obtener saldo pendiente actual (último registrado)
        String sqlCurrentBalance = "SELECT outstanding_balance FROM liability_values WHERE liability_id = ? ORDER BY valuation_date DESC LIMIT 1";
        BigDecimal currentOutstandingBalance = jdbcTemplate.query(sqlCurrentBalance, rs -> {
            if (rs.next()) {
                return BigDecimal.valueOf(rs.getDouble("outstanding_balance"));
            }
            return BigDecimal.ZERO;
        }, liabilityId);

        // Calcular capital pagado
        BigDecimal principalAmount = liability.getPrincipalAmount() != null ? 
                BigDecimal.valueOf(liability.getPrincipalAmount()) : BigDecimal.ZERO;
        BigDecimal principalPaid = principalAmount.subtract(currentOutstandingBalance);

        // Calcular porcentaje de progreso
        BigDecimal progressPercentage = principalAmount.compareTo(BigDecimal.ZERO) > 0 ?
                principalPaid.divide(principalAmount, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        // Contar transacciones
        String sqlCount = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND liability_id = ?";
        Integer transactionCount = jdbcTemplate.queryForObject(sqlCount, Integer.class, userId, liabilityId);

        // Obtener TODAS las transacciones (sin límite)
        String sqlRecent = "SELECT * FROM transactions WHERE user_id = ? AND liability_id = ? ORDER BY transaction_date DESC, transaction_id DESC";
        List<Transaction> recentTransactions = jdbcTemplate.query(sqlRecent, (rs, rowNum) -> mapTransaction(rs), userId, liabilityId);

        // Obtener TODO el historial de valores (sin límite de tiempo)
        String sqlValueHistory = "SELECT * FROM liability_values WHERE liability_id = ? ORDER BY valuation_date DESC";
        List<LiabilityValue> valueHistory = jdbcTemplate.query(sqlValueHistory, (rs, rowNum) -> mapLiabilityValue(rs), liabilityId);

        // Obtener intereses
        List<Interest> interests = getInterests(userId, liabilityId);

        return LiabilityDetail.builder()
                .liability(liability)
                .currentOutstandingBalance(currentOutstandingBalance)
                .principalPaid(principalPaid)
                .progressPercentage(progressPercentage)
                .transactionCount(transactionCount)
                .recentTransactions(recentTransactions)
                .valueHistory(valueHistory)
                .interests(interests)
                .build();
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

    private Liability mapRow(ResultSet rs) throws SQLException {
        Liability l = new Liability();
        l.setLiabilityId(rs.getLong("liability_id"));
        l.setUserId(rs.getLong("user_id"));
        l.setLiabilityTypeId(rs.getLong("liability_type_id"));
        l.setName(rs.getString("name"));
        l.setDescription(rs.getString("description"));
        l.setPrincipalAmount(rs.getDouble("principal_amount"));
        l.setStartDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null);
        l.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        l.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return l;
    }

    private LiabilityValue mapLiabilityValue(ResultSet rs) throws SQLException {
        LiabilityValue lv = new LiabilityValue();
        lv.setLiabilityValueId(rs.getLong("value_id"));
        lv.setLiabilityId(rs.getLong("liability_id"));
        java.sql.Date vd = rs.getDate("valuation_date");
        lv.setValuationDate(vd != null ? vd.toLocalDate() : null);
        java.sql.Date ed = rs.getDate("end_date");
        lv.setEndDate(ed != null ? ed.toLocalDate() : null);
        lv.setOutstandingBalance(rs.getDouble("outstanding_balance"));
        return lv;
    }
}
