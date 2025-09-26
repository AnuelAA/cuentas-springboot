package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.CategoryServicePort;
import com.cuentas.backend.domain.Category;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/categories")
public class CategoriesControllerAdapter {

    private final CategoryServicePort categoryService;

    public CategoriesControllerAdapter(CategoryServicePort categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> listCategories(@PathVariable Long userId) {
        return ResponseEntity.ok(categoryService.listCategories(userId));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<Category> getCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getCategory(userId, categoryId));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@PathVariable Long userId, @RequestBody Category category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(userId, category));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long userId, @PathVariable Long categoryId, @RequestBody Category category) {
        return ResponseEntity.ok(categoryService.updateCategory(userId, categoryId, category));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        categoryService.deleteCategory(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
