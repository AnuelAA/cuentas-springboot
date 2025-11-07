package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Category;
import com.cuentas.backend.domain.CategoryDetail;
import java.util.List;

public interface CategoryServicePort {
    Category createCategory(Long userId, Category category);
    Category getCategory(Long userId, Long categoryId);
    List<Category> listCategories(Long userId);
    Category updateCategory(Long userId, Long categoryId, Category category);
    void deleteCategory(Long userId, Long categoryId);
    List<Category> getSubcategories(Long userId, Long parentCategoryId);
    CategoryDetail getCategoryDetail(Long userId, Long categoryId);
    void reassignTransactions(Long userId, Long fromCategoryId, Long toCategoryId);
}
