package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.CategoryServicePort;
import com.cuentas.backend.domain.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/categories")
public class CategoriesControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CategoriesControllerAdapter.class);

    private final CategoryServicePort categoryService;

    public CategoriesControllerAdapter(CategoryServicePort categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> listCategories(@PathVariable Long userId) {
        logger.info("Listando categorías para userId={}", userId);
        List<Category> categories = categoryService.listCategories(userId);
        logger.info("Respuesta listCategories: {}", categories);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<Category> getCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        logger.info("Obteniendo categoría con categoryId={} para userId={}", categoryId, userId);
        Category category = categoryService.getCategory(userId, categoryId);
        logger.info("Respuesta getCategory: {}", category);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@PathVariable Long userId, @RequestBody Category category) {
        logger.info("Creando categoría para userId={}, category={}", userId, category);
        Category created = categoryService.createCategory(userId, category);
        logger.info("Respuesta createCategory: {}", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long userId, @PathVariable Long categoryId, @RequestBody Category category) {
        logger.info("Actualizando categoría con categoryId={} para userId={}, category={}", categoryId, userId, category);
        Category updated = categoryService.updateCategory(userId, categoryId, category);
        logger.info("Respuesta updateCategory: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        logger.info("Eliminando categoría con categoryId={} para userId={}", categoryId, userId);
        categoryService.deleteCategory(userId, categoryId);
        logger.info("Respuesta deleteCategory: No Content");
        return ResponseEntity.noContent().build();
    }
}
