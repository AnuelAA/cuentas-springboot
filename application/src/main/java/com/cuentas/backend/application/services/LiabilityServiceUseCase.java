package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.LiabilityServicePort;
import com.cuentas.backend.domain.Liability;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class LiabilityServiceUseCase implements LiabilityServicePort {

    private final JdbcTemplate jdbcTemplate;

    public LiabilityServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Liability createLiability(Long userId, Liability liability) {
        String sql = "INSERT INTO liabilities (user_id, liability_type_id, name, description, principal_amount, interest_rate, start_date, end_date, outstanding_balance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING liability_id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                liability.getLiabilityTypeId(),
                liability.getName(),
                liability.getDescription(),
                liability.getPrincipalAmount(),
                liability.getInterestRate(),
                liability.getStartDate(),
                liability.getEndDate(),
                liability.getOutstandingBalance()
        );
        liability.setLiabilityId(id);
        liability.setUserId(userId);
        return liability;
    }

    @Override
    public Liability getLiability(Long userId, Long liabilityId) {
        String sql = "SELECT * FROM liabilities WHERE user_id = ? AND liability_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, liabilityId);
    }

    @Override
    public List<Liability> listLiabilities(Long userId) {
        String sql = "SELECT * FROM liabilities WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    @Override
    public Liability updateLiability(Long userId, Long liabilityId, Liability liability) {
        String sql = "UPDATE liabilities SET name = ?, description = ?, liability_type_id = ?, principal_amount = ?, interest_rate = ?, start_date = ?, end_date = ?, outstanding_balance = ?, updated_at = NOW() " +
                "WHERE user_id = ? AND liability_id = ?";
        jdbcTemplate.update(sql,
                liability.getName(),
                liability.getDescription(),
                liability.getLiabilityTypeId(),
                liability.getPrincipalAmount(),
                liability.getInterestRate(),
                liability.getStartDate(),
                liability.getEndDate(),
                liability.getOutstandingBalance(),
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
        l.setPrincipalAmount(rs.getBigDecimal("principal_amount"));
        l.setInterestRate(rs.getBigDecimal("interest_rate"));
        l.setStartDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null);
        l.setEndDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null);
        l.setOutstandingBalance(rs.getBigDecimal("outstanding_balance"));
        l.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        l.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return l;
    }
}
