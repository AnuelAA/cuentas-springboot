package com.cuentas.backend.adapters;

import com.cuentas.backend.domain.Asset;
import com.cuentas.backend.application.ports.driving.AssetServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/assets")
public class AssetsControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AssetsControllerAdapter.class);

    private final AssetServicePort assetService;

    public AssetsControllerAdapter(AssetServicePort assetService) {
        this.assetService = assetService;
    }

    @GetMapping()
    public ResponseEntity<List<Asset>> listAssets(@PathVariable Long userId) {
        logger.info("Listando assets para userId={}", userId);
        List<Asset> assets = assetService.listAssets(userId);
        logger.info("Respuesta listAssets: {}", assets);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<Asset> getAsset(@PathVariable Long userId, @PathVariable Long assetId) {
        logger.info("Obteniendo asset con assetId={} para userId={}", assetId, userId);
        Asset asset = assetService.getAsset(userId, assetId);
        logger.info("Respuesta getAsset: {}", asset);
        return ResponseEntity.ok(asset);
    }

    @PostMapping
    public ResponseEntity<Asset> createAsset(@PathVariable Long userId, @RequestBody Asset asset) {
        logger.info("Creando asset para userId={}, asset={}", userId, asset);
        Asset created = assetService.createAsset(userId, asset);
        logger.info("Respuesta createAsset: {}", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<Asset> updateAsset(@PathVariable Long userId, @PathVariable Long assetId, @RequestBody Asset asset) {
        logger.info("Actualizando asset con assetId={} para userId={}, asset={}", assetId, userId, asset);
        Asset updated = assetService.updateAsset(userId, assetId, asset);
        logger.info("Respuesta updateAsset: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long userId, @PathVariable Long assetId) {
        logger.info("Eliminando asset con assetId={} para userId={}", assetId, userId);
        assetService.deleteAsset(userId, assetId);
        logger.info("Respuesta deleteAsset: No Content");
        return ResponseEntity.noContent().build();
    }
}
