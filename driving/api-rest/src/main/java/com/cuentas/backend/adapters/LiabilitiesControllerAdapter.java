package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.LiabilityServicePort;
import com.cuentas.backend.domain.Interest;
import com.cuentas.backend.domain.Liability;
import com.cuentas.backend.domain.LiabilityDetail;
import com.cuentas.backend.domain.LiabilityValue;
import com.cuentas.backend.domain.CreateLiabilityValueRequest;
import com.cuentas.backend.domain.CreateInterestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/liabilities")
public class LiabilitiesControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LiabilitiesControllerAdapter.class);

    private final LiabilityServicePort liabilityService;

    public LiabilitiesControllerAdapter(LiabilityServicePort liabilityService) {
        this.liabilityService = liabilityService;
    }

    @GetMapping
    public ResponseEntity<List<Liability>> listLiabilities(@PathVariable Long userId) {
        logger.info("Listando liabilities para userId={}", userId);
        List<Liability> liabilities = liabilityService.listLiabilities(userId);
        logger.info("Respuesta listLiabilities: {}", liabilities);
        return ResponseEntity.ok(liabilities);
    }

    @GetMapping("/{liabilityId}")
    public ResponseEntity<Liability> getLiability(@PathVariable Long userId, @PathVariable Long liabilityId) {
        logger.info("Obteniendo liability con liabilityId={} para userId={}", liabilityId, userId);
        Liability liability = liabilityService.getLiability(userId, liabilityId);
        logger.info("Respuesta getLiability: {}", liability);
        return ResponseEntity.ok(liability);
    }

    @PostMapping
    public ResponseEntity<Liability> createLiability(@PathVariable Long userId, @RequestBody Liability liability) {
        logger.info("Creando liability para userId={}, liability={}", userId, liability);
        Liability created = liabilityService.createLiability(userId, liability);
        logger.info("Respuesta createLiability: {}", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{liabilityId}")
    public ResponseEntity<Liability> updateLiability(@PathVariable Long userId, @PathVariable Long liabilityId, @RequestBody Liability liability) {
        logger.info("Actualizando liability con liabilityId={} para userId={}, liability={}", liabilityId, userId, liability);
        Liability updated = liabilityService.updateLiability(userId, liabilityId, liability);
        logger.info("Respuesta updateLiability: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{liabilityId}")
    public ResponseEntity<Void> deleteLiability(@PathVariable Long userId, @PathVariable Long liabilityId) {
        logger.info("Eliminando liability con liabilityId={} para userId={}", liabilityId, userId);
        liabilityService.deleteLiability(userId, liabilityId);
        logger.info("Respuesta deleteLiability: No Content");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{liabilityId}/values")
    public ResponseEntity<LiabilityValue> createLiabilityValue(
            @PathVariable Long userId,
            @PathVariable Long liabilityId,
            @RequestBody CreateLiabilityValueRequest request) {
        logger.info("Creando/actualizando snapshot para userId={}, liabilityId={}, fecha={}", userId, liabilityId, request.getValuationDate());
        try {
            LiabilityValue liabilityValue = liabilityService.upsertLiabilityValue(
                    userId,
                    liabilityId,
                    request.getValuationDate(),
                    request.getOutstandingBalance(),
                    request.getEndDate()
            );
            logger.info("Snapshot creado/actualizado: {}", liabilityValue);
            return ResponseEntity.ok(liabilityValue);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al crear/actualizar snapshot: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{liabilityId}/interests")
    public ResponseEntity<Interest> createInterest(
            @PathVariable Long userId,
            @PathVariable Long liabilityId,
            @RequestBody CreateInterestRequest request) {
        logger.info("Creando interés para userId={}, liabilityId={}, tipo={}, tasa={}, fechaInicio={}", 
                userId, liabilityId, request.getType(), request.getAnnualRate(), request.getStartDate());
        try {
            Interest interest = liabilityService.createInterest(
                    userId,
                    liabilityId,
                    request.getType(),
                    request.getAnnualRate(),
                    request.getStartDate()
            );
            logger.info("Interés creado: {}", interest);
            return ResponseEntity.status(HttpStatus.CREATED).body(interest);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al crear interés: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al crear interés: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{liabilityId}/interests")
    public ResponseEntity<List<Interest>> getInterests(
            @PathVariable Long userId,
            @PathVariable Long liabilityId) {
        logger.info("Consultando intereses para userId={}, liabilityId={}", userId, liabilityId);
        try {
            List<Interest> interests = liabilityService.getInterests(userId, liabilityId);
            logger.info("Respuesta getInterests: {} intereses encontrados", interests.size());
            return ResponseEntity.ok(interests);
        } catch (RuntimeException e) {
            logger.error("Error al consultar intereses: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{liabilityId}/interests/{interestId}")
    public ResponseEntity<Interest> updateInterest(
            @PathVariable Long userId,
            @PathVariable Long liabilityId,
            @PathVariable Long interestId,
            @RequestBody CreateInterestRequest request) {
        logger.info("Actualizando interés interestId={} para userId={}, liabilityId={}, tipo={}, tasa={}, fechaInicio={}", 
                interestId, userId, liabilityId, request.getType(), request.getAnnualRate(), request.getStartDate());
        try {
            Interest interest = liabilityService.updateInterest(
                    userId,
                    liabilityId,
                    interestId,
                    request.getType(),
                    request.getAnnualRate(),
                    request.getStartDate()
            );
            logger.info("Interés actualizado: {}", interest);
            return ResponseEntity.ok(interest);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al actualizar interés: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al actualizar interés: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{liabilityId}/interests/{interestId}")
    public ResponseEntity<Void> deleteInterest(
            @PathVariable Long userId,
            @PathVariable Long liabilityId,
            @PathVariable Long interestId) {
        logger.info("Eliminando interés interestId={} para userId={}, liabilityId={}", interestId, userId, liabilityId);
        try {
            liabilityService.deleteInterest(userId, liabilityId, interestId);
            logger.info("Interés eliminado exitosamente");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error al eliminar interés: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{liabilityId}/detail")
    public ResponseEntity<LiabilityDetail> getLiabilityDetail(@PathVariable Long userId, @PathVariable Long liabilityId) {
        logger.info("Obteniendo detalle de liabilityId={} para userId={}", liabilityId, userId);
        try {
            LiabilityDetail detail = liabilityService.getLiabilityDetail(userId, liabilityId);
            logger.info("Respuesta getLiabilityDetail: saldoPendiente={}, capitalPagado={}, progreso={}%", 
                    detail.getCurrentOutstandingBalance(), detail.getPrincipalPaid(), detail.getProgressPercentage());
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            logger.error("Error al obtener detalle de pasivo: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
