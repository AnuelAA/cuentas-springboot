package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.LiabilityServicePort;
import com.cuentas.backend.domain.Liability;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/liabilities")
public class LiabilitiesControllerAdapter {

    private final LiabilityServicePort liabilityService;

    public LiabilitiesControllerAdapter(LiabilityServicePort liabilityService) {
        this.liabilityService = liabilityService;
    }

    @GetMapping
    public ResponseEntity<List<Liability>> listLiabilities(@PathVariable Long userId) {
        return ResponseEntity.ok(liabilityService.listLiabilities(userId));
    }

    @GetMapping("/{liabilityId}")
    public ResponseEntity<Liability> getLiability(@PathVariable Long userId, @PathVariable Long liabilityId) {
        return ResponseEntity.ok(liabilityService.getLiability(userId, liabilityId));
    }

    @PostMapping
    public ResponseEntity<Liability> createLiability(@PathVariable Long userId, @RequestBody Liability liability) {
        return ResponseEntity.status(HttpStatus.CREATED).body(liabilityService.createLiability(userId, liability));
    }

    @PutMapping("/{liabilityId}")
    public ResponseEntity<Liability> updateLiability(@PathVariable Long userId, @PathVariable Long liabilityId, @RequestBody Liability liability) {
        return ResponseEntity.ok(liabilityService.updateLiability(userId, liabilityId, liability));
    }

    @DeleteMapping("/{liabilityId}")
    public ResponseEntity<Void> deleteLiability(@PathVariable Long userId, @PathVariable Long liabilityId) {
        liabilityService.deleteLiability(userId, liabilityId);
        return ResponseEntity.noContent().build();
    }
}
