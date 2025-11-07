package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.CategoryServicePort;
import com.cuentas.backend.domain.Category;
import com.cuentas.backend.domain.CategoryDetail;
import com.cuentas.backend.domain.ReassignTransactionsRequest;
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
    public ResponseEntity<?> deleteCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        logger.info("Eliminando categoría con categoryId={} para userId={}", categoryId, userId);
        try {
            categoryService.deleteCategory(userId, categoryId);
            logger.info("Respuesta deleteCategory: No Content");
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            logger.warn("No se puede eliminar categoría: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error al eliminar categoría: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Error al eliminar la categoría"));
        }
    }

    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<List<Category>> getSubcategories(@PathVariable Long userId, @PathVariable Long categoryId) {
        logger.info("Obteniendo subcategorías de categoryId={} para userId={}", categoryId, userId);
        List<Category> subcategories = categoryService.getSubcategories(userId, categoryId);
        logger.info("Respuesta getSubcategories: {} subcategorías encontradas", subcategories.size());
        return ResponseEntity.ok(subcategories);
    }

    @GetMapping("/{categoryId}/detail")
    public ResponseEntity<CategoryDetail> getCategoryDetail(@PathVariable Long userId, @PathVariable Long categoryId) {
        logger.info("Obteniendo detalle de categoryId={} para userId={}", categoryId, userId);
        try {
            CategoryDetail detail = categoryService.getCategoryDetail(userId, categoryId);
            logger.info("Respuesta getCategoryDetail: ingresos={}, gastos={}, transacciones={}", 
                    detail.getTotalIncome(), detail.getTotalExpenses(), detail.getTransactionCount());
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            logger.error("Error al obtener detalle de categoría: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{categoryId}/reassign-transactions")
    public ResponseEntity<?> reassignTransactions(
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            @RequestBody ReassignTransactionsRequest request) {
        logger.info("Reasignando transacciones de categoryId={} a categoryId={} para userId={}", 
                categoryId, request.getToCategoryId(), userId);
        try {
            categoryService.reassignTransactions(userId, categoryId, request.getToCategoryId());
            logger.info("Transacciones reasignadas exitosamente");
            return ResponseEntity.ok(java.util.Map.of("message", "Transacciones reasignadas correctamente"));
        } catch (RuntimeException e) {
            logger.error("Error al reasignar transacciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
