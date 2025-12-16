package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.TransactionTemplateServicePort;
import com.cuentas.backend.domain.TransactionTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionTemplateServiceUseCase implements TransactionTemplateServicePort {

    private final JdbcTemplate jdbcTemplate;

    public TransactionTemplateServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TransactionTemplate> getTransactionTemplates(Long userId) {
        String sql = "SELECT * FROM transaction_templates WHERE user_id = ? ORDER BY name";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    @Override
    @Transactional
    public TransactionTemplate createTransactionTemplate(Long userId, TransactionTemplate template) {
        // Validaciones
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la plantilla es obligatorio");
        }

        if (template.getType() == null || (!template.getType().equals("income") && !template.getType().equals("expense"))) {
            throw new IllegalArgumentException("El tipo debe ser 'income' o 'expense'");
        }

        if (template.getAmount() == null || template.getAmount() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        // Manejar categoría: si categoryId no se proporciona pero categoryName sí, crear la categoría
        Long categoryId = template.getCategoryId();
        if (categoryId == null && template.getCategoryName() != null && !template.getCategoryName().trim().isEmpty()) {
            categoryId = findOrCreateCategory(userId, template.getCategoryName());
        }

        // Validar que categoryId existe y pertenece al usuario (si se proporciona)
        if (categoryId != null) {
            String checkCategorySql = "SELECT category_id FROM categories WHERE category_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkCategorySql, Long.class, categoryId, userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("La categoría no existe o no pertenece al usuario");
            }
        }

        // Validar activos y pasivos si se proporcionan
        if (template.getAssetId() != null) {
            String checkAssetSql = "SELECT asset_id FROM assets WHERE asset_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkAssetSql, Long.class, template.getAssetId(), userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("El activo no existe o no pertenece al usuario");
            }
        }

        if (template.getRelatedAssetId() != null) {
            String checkAssetSql = "SELECT asset_id FROM assets WHERE asset_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkAssetSql, Long.class, template.getRelatedAssetId(), userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("El activo relacionado no existe o no pertenece al usuario");
            }
        }

        if (template.getLiabilityId() != null) {
            String checkLiabilitySql = "SELECT liability_id FROM liabilities WHERE liability_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkLiabilitySql, Long.class, template.getLiabilityId(), userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("El pasivo no existe o no pertenece al usuario");
            }
        }

        // Obtener nombre de categoría si categoryId está presente
        String categoryName = null;
        if (categoryId != null) {
            categoryName = getCategoryName(categoryId);
        } else if (template.getCategoryName() != null) {
            categoryName = template.getCategoryName();
        }

        String sql = "INSERT INTO transaction_templates (user_id, name, category_id, category_name, type, amount, asset_id, related_asset_id, liability_id, description, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING template_id";

        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                template.getName(),
                categoryId,
                categoryName,
                template.getType(),
                template.getAmount(),
                template.getAssetId(),
                template.getRelatedAssetId(),
                template.getLiabilityId(),
                template.getDescription()
        );

        template.setTemplateId(id);
        template.setUserId(userId);
        template.setCategoryId(categoryId);
        template.setCategoryName(categoryName);
        return template;
    }

    @Override
    @Transactional
    public TransactionTemplate updateTransactionTemplate(Long userId, Long templateId, TransactionTemplate template) {
        // Validar que la plantilla existe y pertenece al usuario
        String checkSql = "SELECT template_id FROM transaction_templates WHERE template_id = ? AND user_id = ?";
        try {
            jdbcTemplate.queryForObject(checkSql, Long.class, templateId, userId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Plantilla no encontrada o no pertenece al usuario");
        }

        // Construir UPDATE dinámicamente
        List<Object> params = new ArrayList<>();
        List<String> updates = new ArrayList<>();

        if (template.getName() != null) {
            if (template.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre de la plantilla no puede estar vacío");
            }
            updates.add("name = ?");
            params.add(template.getName());
        }

        if (template.getType() != null) {
            if (!template.getType().equals("income") && !template.getType().equals("expense")) {
                throw new IllegalArgumentException("El tipo debe ser 'income' o 'expense'");
            }
            updates.add("type = ?");
            params.add(template.getType());
        }

        if (template.getAmount() != null) {
            if (template.getAmount() <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor a 0");
            }
            updates.add("amount = ?");
            params.add(template.getAmount());
        }

        // Manejar categoría
        if (template.getCategoryId() != null || (template.getCategoryName() != null && !template.getCategoryName().trim().isEmpty())) {
            Long categoryId = template.getCategoryId();
            if (categoryId == null && template.getCategoryName() != null) {
                categoryId = findOrCreateCategory(userId, template.getCategoryName());
            }

            // Validar que la categoría pertenece al usuario
            if (categoryId != null) {
                String checkCategorySql = "SELECT category_id FROM categories WHERE category_id = ? AND user_id = ?";
                try {
                    jdbcTemplate.queryForObject(checkCategorySql, Long.class, categoryId, userId);
                } catch (DataAccessException e) {
                    throw new IllegalArgumentException("La categoría no existe o no pertenece al usuario");
                }
            }

            String categoryName = categoryId != null ? getCategoryName(categoryId) : template.getCategoryName();
            updates.add("category_id = ?");
            params.add(categoryId);
            updates.add("category_name = ?");
            params.add(categoryName);
        }

        // Nota: Para assetId, relatedAssetId y liabilityId, solo actualizamos si se proporciona explícitamente
        // Si no se proporciona, no se modifica el valor existente
        if (template.getAssetId() != null) {
            // Validar activo
            String checkAssetSql = "SELECT asset_id FROM assets WHERE asset_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkAssetSql, Long.class, template.getAssetId(), userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("El activo no existe o no pertenece al usuario");
            }
            updates.add("asset_id = ?");
            params.add(template.getAssetId());
        }

        if (template.getRelatedAssetId() != null) {
            // Validar activo relacionado
            String checkAssetSql = "SELECT asset_id FROM assets WHERE asset_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkAssetSql, Long.class, template.getRelatedAssetId(), userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("El activo relacionado no existe o no pertenece al usuario");
            }
            updates.add("related_asset_id = ?");
            params.add(template.getRelatedAssetId());
        }

        if (template.getLiabilityId() != null) {
            // Validar pasivo
            String checkLiabilitySql = "SELECT liability_id FROM liabilities WHERE liability_id = ? AND user_id = ?";
            try {
                jdbcTemplate.queryForObject(checkLiabilitySql, Long.class, template.getLiabilityId(), userId);
            } catch (DataAccessException e) {
                throw new IllegalArgumentException("El pasivo no existe o no pertenece al usuario");
            }
            updates.add("liability_id = ?");
            params.add(template.getLiabilityId());
        }

        if (template.getDescription() != null) {
            updates.add("description = ?");
            params.add(template.getDescription());
        }

        if (updates.isEmpty()) {
            // No hay nada que actualizar
            return getTransactionTemplate(userId, templateId);
        }

        updates.add("updated_at = CURRENT_TIMESTAMP");
        params.add(userId);
        params.add(templateId);

        String sql = "UPDATE transaction_templates SET " + String.join(", ", updates) +
                " WHERE user_id = ? AND template_id = ?";

        jdbcTemplate.update(sql, params.toArray());

        return getTransactionTemplate(userId, templateId);
    }

    @Override
    @Transactional
    public void deleteTransactionTemplate(Long userId, Long templateId) {
        String sql = "DELETE FROM transaction_templates WHERE user_id = ? AND template_id = ?";
        int deleted = jdbcTemplate.update(sql, userId, templateId);
        
        if (deleted == 0) {
            throw new RuntimeException("Plantilla no encontrada o no pertenece al usuario");
        }
    }

    private TransactionTemplate getTransactionTemplate(Long userId, Long templateId) {
        String sql = "SELECT * FROM transaction_templates WHERE user_id = ? AND template_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, templateId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Plantilla no encontrada");
        }
    }

    private Long findOrCreateCategory(Long userId, String categoryName) {
        // Buscar categoría existente
        String findSql = "SELECT category_id FROM categories WHERE user_id = ? AND name = ?";
        try {
            return jdbcTemplate.queryForObject(findSql, Long.class, userId, categoryName);
        } catch (DataAccessException e) {
            // No existe, crear nueva
            String createSql = "INSERT INTO categories (user_id, name, created_at) VALUES (?, ?, CURRENT_TIMESTAMP) RETURNING category_id";
            return jdbcTemplate.queryForObject(createSql, Long.class, userId, categoryName);
        }
    }

    private String getCategoryName(Long categoryId) {
        try {
            String sql = "SELECT name FROM categories WHERE category_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, categoryId);
        } catch (DataAccessException e) {
            return null;
        }
    }

    private TransactionTemplate mapRow(ResultSet rs) throws SQLException {
        TransactionTemplate template = new TransactionTemplate();
        template.setTemplateId(rs.getLong("template_id"));
        template.setUserId(rs.getLong("user_id"));
        template.setName(rs.getString("name"));
        
        Long categoryId = rs.getObject("category_id") != null ? rs.getLong("category_id") : null;
        template.setCategoryId(categoryId);
        template.setCategoryName(rs.getString("category_name"));
        
        template.setType(rs.getString("type"));
        template.setAmount(rs.getDouble("amount"));
        
        Long assetId = rs.getObject("asset_id") != null ? rs.getLong("asset_id") : null;
        template.setAssetId(assetId);
        
        Long relatedAssetId = rs.getObject("related_asset_id") != null ? rs.getLong("related_asset_id") : null;
        template.setRelatedAssetId(relatedAssetId);
        
        Long liabilityId = rs.getObject("liability_id") != null ? rs.getLong("liability_id") : null;
        template.setLiabilityId(liabilityId);
        
        template.setDescription(rs.getString("description"));
        
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        template.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        template.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        
        return template;
    }
}

