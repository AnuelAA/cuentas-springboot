package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.AssetServicePort;
import com.cuentas.backend.domain.Asset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

    private Asset mapRow(ResultSet rs) throws SQLException {
        Asset asset = new Asset();
        asset.setAssetId(rs.getLong("asset_id"));
        asset.setUserId(rs.getLong("user_id"));
        asset.setAssetTypeId(rs.getLong("asset_type_id"));
        asset.setName(rs.getString("name"));
        asset.setDescription(rs.getString("description"));
        asset.setAcquisitionDate(rs.getDate("acquisition_date") != null ? rs.getDate("acquisition_date").toLocalDate() : null);
        asset.setAcquisitionValue(rs.getBigDecimal("acquisition_value"));
        asset.setCurrentValue(rs.getBigDecimal("current_value"));
        return asset;
    }
}
