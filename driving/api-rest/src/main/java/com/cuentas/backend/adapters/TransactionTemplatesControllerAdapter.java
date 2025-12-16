package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.TransactionTemplateServicePort;
import com.cuentas.backend.domain.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/transaction-templates")
public class TransactionTemplatesControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTemplatesControllerAdapter.class);

    private final TransactionTemplateServicePort templateService;

    public TransactionTemplatesControllerAdapter(TransactionTemplateServicePort templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionTemplate>> getTransactionTemplates(@PathVariable Long userId) {
        logger.info("Obteniendo plantillas de transacciones para userId={}", userId);
        try {
            List<TransactionTemplate> templates = templateService.getTransactionTemplates(userId);
            logger.info("Respuesta getTransactionTemplates: {} plantillas encontradas", templates.size());
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("Error al obtener plantillas de transacciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<TransactionTemplate> createTransactionTemplate(
            @PathVariable Long userId,
            @RequestBody TransactionTemplate template) {
        logger.info("Creando plantilla de transacción para userId={}, template={}", userId, template);
        try {
            TransactionTemplate created = templateService.createTransactionTemplate(userId, template);
            logger.info("Plantilla creada: {}", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al crear plantilla: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error al crear plantilla: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TransactionTemplate> updateTransactionTemplate(
            @PathVariable Long userId,
            @PathVariable Long templateId,
            @RequestBody TransactionTemplate template) {
        logger.info("Actualizando plantilla para userId={}, templateId={}, template={}", userId, templateId, template);
        try {
            TransactionTemplate updated = templateService.updateTransactionTemplate(userId, templateId, template);
            logger.info("Plantilla actualizada: {}", updated);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al actualizar plantilla: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al actualizar plantilla: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error inesperado al actualizar plantilla: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTransactionTemplate(
            @PathVariable Long userId,
            @PathVariable Long templateId) {
        logger.info("Eliminando plantilla para userId={}, templateId={}", userId, templateId);
        try {
            templateService.deleteTransactionTemplate(userId, templateId);
            logger.info("Plantilla eliminada correctamente");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error al eliminar plantilla: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error inesperado al eliminar plantilla: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

