package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.LiabilityTypeServicePort;
import com.cuentas.backend.domain.LiabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/liability-types")
public class LiabilityTypesControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LiabilityTypesControllerAdapter.class);

    private final LiabilityTypeServicePort liabilityTypeService;

    public LiabilityTypesControllerAdapter(LiabilityTypeServicePort liabilityTypeService) {
        this.liabilityTypeService = liabilityTypeService;
    }

    @GetMapping
    public ResponseEntity<List<LiabilityType>> getAllLiabilityTypes() {
        logger.info("Obteniendo todos los tipos de pasivos");
        try {
            List<LiabilityType> liabilityTypes = liabilityTypeService.getAllLiabilityTypes();
            logger.info("Respuesta getAllLiabilityTypes: {} tipos encontrados", liabilityTypes.size());
            return ResponseEntity.ok(liabilityTypes);
        } catch (Exception e) {
            logger.error("Error al obtener tipos de pasivos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

