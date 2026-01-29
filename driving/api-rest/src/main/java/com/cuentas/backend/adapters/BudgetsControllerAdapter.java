package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.BudgetServicePort;
import com.cuentas.backend.domain.Budget;
import com.cuentas.backend.domain.BudgetStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<Budget>> listBudgets(@PathVariable Long userId) {
        logger.info("Listando presupuestos para userId={}", userId);
        List<Budget> budgets = budgetService.listBudgets(userId);
        logger.info("Respuesta listBudgets: {} presupuestos encontrados", budgets.size());
        return ResponseEntity.ok(budgets);
    }

    @GetMapping("/{budgetId}")
    public ResponseEntity<Budget> getBudget(@PathVariable Long userId, @PathVariable Long budgetId) {
        logger.info("Obteniendo presupuesto con budgetId={} para userId={}", budgetId, userId);
        try {
            Budget budget = budgetService.getBudget(userId, budgetId);
            logger.info("Respuesta getBudget: {}", budget);
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            logger.error("Error al obtener presupuesto: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@PathVariable Long userId, @RequestBody Budget budget) {
        logger.info("Creando presupuesto para userId={}, budget={}", userId, budget);
        Budget created = budgetService.createBudget(userId, budget);
        logger.info("Respuesta createBudget: {}", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long userId, @PathVariable Long budgetId, @RequestBody Budget budget) {
        logger.info("Actualizando presupuesto con budgetId={} para userId={}, budget={}", budgetId, userId, budget);
        try {
            Budget updated = budgetService.updateBudget(userId, budgetId, budget);
            logger.info("Respuesta updateBudget: {}", updated);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error al actualizar presupuesto: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long userId, @PathVariable Long budgetId) {
        logger.info("Eliminando presupuesto con budgetId={} para userId={}", budgetId, userId);
        try {
            budgetService.deleteBudget(userId, budgetId);
            logger.info("Presupuesto eliminado exitosamente");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error al eliminar presupuesto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Error al eliminar el presupuesto"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<BudgetStatus> getBudgetStatus(@PathVariable Long userId) {
        logger.info("Obteniendo estado de presupuestos para userId={}", userId);
        BudgetStatus status = budgetService.getBudgetStatus(userId);
        logger.info("Respuesta getBudgetStatus: {} presupuestos activos", status.getItems().size());
        return ResponseEntity.ok(status);
    }
}

