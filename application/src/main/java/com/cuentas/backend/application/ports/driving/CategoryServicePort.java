package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Category;
import java.util.List;

public interface CategoryServicePort {
    Category createCategory(Long userId, Category category);
    Category getCategory(Long userId, Long categoryId);
    List<Category> listCategories(Long userId);
    Category updateCategory(Long userId, Long categoryId, Category category);
    void deleteCategory(Long userId, Long categoryId);
}
