package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.CategoryServicePort;
import com.cuentas.backend.domain.Category;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
        String sql = "INSERT INTO categories (user_id, name, description, type) VALUES (?, ?, ?, ?) RETURNING category_id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class, userId, category.getName(), category.getDescription(), category.getType());
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
        String sql = "UPDATE categories SET name = ?, description = ?, type = ?, updated_at = NOW() WHERE user_id = ? AND category_id = ?";
        jdbcTemplate.update(sql, category.getName(), category.getDescription(), category.getType(), userId, categoryId);
        return getCategory(userId, categoryId);
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        String sql = "DELETE FROM categories WHERE user_id = ? AND category_id = ?";
        jdbcTemplate.update(sql, userId, categoryId);
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getLong("category_id"));
        c.setUserId(rs.getLong("user_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setType(rs.getString("type"));
        c.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        c.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return c;
    }
}