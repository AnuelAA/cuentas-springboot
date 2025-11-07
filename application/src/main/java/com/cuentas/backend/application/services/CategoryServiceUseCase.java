package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.CategoryServicePort;
import com.cuentas.backend.domain.Category;
import com.cuentas.backend.domain.CategoryDetail;
import com.cuentas.backend.domain.Transaction;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class CategoryServiceUseCase implements CategoryServicePort {

    private final JdbcTemplate jdbcTemplate;

    public CategoryServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Category createCategory(Long userId, Category category) {
        String sql = "INSERT INTO categories (user_id, name, description, parent_category_id) VALUES (?, ?, ?, ?) RETURNING category_id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class, userId, category.getName(), category.getDescription(), category.getParentCategoryId());
        category.setCategoryId(id);
        category.setUserId(userId);
        return category;
    }

    @Override
    public Category getCategory(Long userId, Long categoryId) {
        String sql = "SELECT * FROM categories WHERE user_id = ? AND category_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, categoryId);
    }

    @Override
    public List<Category> listCategories(Long userId) {
        String sql = "SELECT * FROM categories WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    @Override
    public Category updateCategory(Long userId, Long categoryId, Category category) {
        String sql = "UPDATE categories SET name = ?, description = ?, parent_category_id = ?, updated_at = NOW() WHERE user_id = ? AND category_id = ?";
        jdbcTemplate.update(sql, category.getName(), category.getDescription(), category.getParentCategoryId(), userId, categoryId);
        return getCategory(userId, categoryId);
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        // Verificar si la categoría tiene transacciones asociadas
        String sqlCount = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND category_id = ?";
        Integer transactionCount = jdbcTemplate.queryForObject(sqlCount, Integer.class, userId, categoryId);
        
        if (transactionCount != null && transactionCount > 0) {
            throw new IllegalStateException("No se puede eliminar la categoría porque tiene " + transactionCount + 
                    " transacciones asociadas. Primero reasigna las transacciones a otra categoría.");
        }
        
        String sql = "DELETE FROM categories WHERE user_id = ? AND category_id = ?";
        jdbcTemplate.update(sql, userId, categoryId);
    }

    @Override
    public void reassignTransactions(Long userId, Long fromCategoryId, Long toCategoryId) {
        // Validar que ambas categorías existen y pertenecen al usuario
        try {
            getCategory(userId, fromCategoryId);
            getCategory(userId, toCategoryId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Una o ambas categorías no existen o no pertenecen al usuario");
        }
        
        // Reasignar todas las transacciones de fromCategoryId a toCategoryId
        String sql = "UPDATE transactions SET category_id = ?, updated_at = NOW() WHERE user_id = ? AND category_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, toCategoryId, userId, fromCategoryId);
        
        if (rowsAffected == 0) {
            throw new RuntimeException("No hay transacciones para reasignar");
        }
    }

    @Override
    public List<Category> getSubcategories(Long userId, Long parentCategoryId) {
        String sql = "SELECT * FROM categories WHERE user_id = ? AND parent_category_id = ? ORDER BY name";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId, parentCategoryId);
    }

    @Override
    public CategoryDetail getCategoryDetail(Long userId, Long categoryId) {
        // Validar que la categoría existe y pertenece al usuario
        Category category;
        try {
            category = getCategory(userId, categoryId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Categoría no encontrada o no pertenece al usuario");
        }

        // Obtener subcategorías
        List<Category> subcategories = getSubcategories(userId, categoryId);

        // Calcular totales de ingresos y gastos
        String sqlIncome = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND category_id = ? AND transaction_type = 'income'";
        String sqlExpense = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = ? AND category_id = ? AND transaction_type = 'expense'";
        
        BigDecimal totalIncome = jdbcTemplate.queryForObject(sqlIncome, BigDecimal.class, userId, categoryId);
        BigDecimal totalExpenses = jdbcTemplate.queryForObject(sqlExpense, BigDecimal.class, userId, categoryId);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        // Contar transacciones
        String sqlCount = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND category_id = ?";
        Integer transactionCount = jdbcTemplate.queryForObject(sqlCount, Integer.class, userId, categoryId);

        // Obtener TODAS las transacciones (sin límite)
        String sqlRecent = "SELECT * FROM transactions WHERE user_id = ? AND category_id = ? ORDER BY transaction_date DESC, transaction_id DESC";
        List<Transaction> recentTransactions = jdbcTemplate.query(sqlRecent, (rs, rowNum) -> mapTransaction(rs), userId, categoryId);

        return CategoryDetail.builder()
                .category(category)
                .subcategories(subcategories)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .transactionCount(transactionCount)
                .recentTransactions(recentTransactions)
                .build();
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("transaction_id"));
        t.setUserId(rs.getLong("user_id"));
        t.setCategoryId(rs.getLong("category_id"));
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

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getLong("category_id"));
        c.setUserId(rs.getLong("user_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        Long parentId = rs.getLong("parent_category_id");
        c.setParentCategoryId(rs.wasNull() ? null : parentId);
        c.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        c.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return c;
    }
}