package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.LiabilityServicePort;
import com.cuentas.backend.domain.Liability;
import com.cuentas.backend.domain.LiabilityValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String sql = "UPDATE liabilities SET name = ?, description = ?, liability_type_id = ?, principal_amount = ?, start_date = ?, updated_at = NOW() " +
                "WHERE user_id = ? AND liability_id = ?";
        jdbcTemplate.update(sql,
                liability.getName(),
                liability.getDescription(),
                liability.getLiabilityTypeId(),
                liability.getPrincipalAmount(),
                liability.getStartDate(),
                userId,
                liabilityId
        );
        return getLiability(userId, liabilityId);
    }

    @Override
    public void deleteLiability(Long userId, Long liabilityId) {
        String sql = "DELETE FROM liabilities WHERE user_id = ? AND liability_id = ?";
        jdbcTemplate.update(sql, userId, liabilityId);
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
