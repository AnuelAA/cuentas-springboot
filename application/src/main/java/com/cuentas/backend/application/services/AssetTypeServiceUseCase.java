package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.AssetTypeServicePort;
import com.cuentas.backend.domain.AssetType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class AssetTypeServiceUseCase implements AssetTypeServicePort {

    private final JdbcTemplate jdbcTemplate;

    public AssetTypeServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AssetType> getAllAssetTypes() {
        String sql = "SELECT asset_type_id, name, description, is_checking_account FROM asset_types ORDER BY asset_type_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
    }

    private AssetType mapRow(ResultSet rs) throws SQLException {
        AssetType assetType = new AssetType();
        assetType.setAssetTypeId(rs.getLong("asset_type_id"));
        assetType.setName(rs.getString("name"));
        assetType.setDescription(rs.getString("description"));
        assetType.setIsCheckingAccount(rs.getBoolean("is_checking_account"));
        return assetType;
    }
}

