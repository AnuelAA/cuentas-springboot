package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.LiabilityServicePort;
import com.cuentas.backend.domain.Liability;
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
}
