package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.BudgetServicePort;
import com.cuentas.backend.domain.Budget;
import com.cuentas.backend.domain.BudgetStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/budgets")
public class BudgetsControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BudgetsControllerAdapter.class);

    private final BudgetServicePort budgetService;

    public BudgetsControllerAdapter(BudgetServicePort budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        logger.info("Obteniendo presupuestos para userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        try {
            List<Budget> budgets = budgetService.getBudgets(userId, startDate, endDate);
            logger.info("Respuesta getBudgets: {} presupuestos encontrados", budgets.size());
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            logger.error("Error al obtener presupuestos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<List<BudgetStatus>> getBudgetsStatus(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        logger.info("Obteniendo estado de presupuestos para userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        try {
            List<BudgetStatus> statusList = budgetService.getBudgetsStatus(userId, startDate, endDate);
            logger.info("Respuesta getBudgetsStatus: {} estados encontrados", statusList.size());
            return ResponseEntity.ok(statusList);
        } catch (Exception e) {
            logger.error("Error al obtener estado de presupuestos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(
            @PathVariable Long userId,
            @RequestBody Budget budget) {
        logger.info("Creando presupuesto para userId={}, budget={}", userId, budget);
        try {
            Budget created = budgetService.createBudget(userId, budget);
            logger.info("Presupuesto creado: {}", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al crear presupuesto: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error al crear presupuesto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable Long userId,
            @PathVariable Long budgetId,
            @RequestBody Budget budget) {
        logger.info("Actualizando presupuesto para userId={}, budgetId={}, budget={}", userId, budgetId, budget);
        try {
            Budget updated = budgetService.updateBudget(userId, budgetId, budget);
            logger.info("Presupuesto actualizado: {}", updated);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al actualizar presupuesto: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al actualizar presupuesto: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error inesperado al actualizar presupuesto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long userId,
            @PathVariable Long budgetId) {
        logger.info("Eliminando presupuesto para userId={}, budgetId={}", userId, budgetId);
        try {
            budgetService.deleteBudget(userId, budgetId);
            logger.info("Presupuesto eliminado correctamente");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error al eliminar presupuesto: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error inesperado al eliminar presupuesto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

