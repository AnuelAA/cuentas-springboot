package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.LiabilityTypeServicePort;
import com.cuentas.backend.domain.LiabilityType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class LiabilityTypeServiceUseCase implements LiabilityTypeServicePort {

    private final JdbcTemplate jdbcTemplate;

    public LiabilityTypeServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<LiabilityType> getAllLiabilityTypes() {
        String sql = "SELECT liability_type_id, name, description FROM liability_types ORDER BY liability_type_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
    }

    private LiabilityType mapRow(ResultSet rs) throws SQLException {
        LiabilityType liabilityType = new LiabilityType();
        liabilityType.setLiabilityTypeId(rs.getLong("liability_type_id"));
        liabilityType.setName(rs.getString("name"));
        liabilityType.setDescription(rs.getString("description"));
        return liabilityType;
    }
}

