package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.AssetTypeServicePort;
import com.cuentas.backend.domain.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-types")
public class AssetTypesControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AssetTypesControllerAdapter.class);

    private final AssetTypeServicePort assetTypeService;

    public AssetTypesControllerAdapter(AssetTypeServicePort assetTypeService) {
        this.assetTypeService = assetTypeService;
    }

    @GetMapping
    public ResponseEntity<List<AssetType>> getAllAssetTypes() {
        logger.info("Obteniendo todos los tipos de activos");
        try {
            List<AssetType> assetTypes = assetTypeService.getAllAssetTypes();
            logger.info("Respuesta getAllAssetTypes: {} tipos encontrados", assetTypes.size());
            return ResponseEntity.ok(assetTypes);
        } catch (Exception e) {
            logger.error("Error al obtener tipos de activos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

