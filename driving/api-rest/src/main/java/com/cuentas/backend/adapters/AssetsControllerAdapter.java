package com.cuentas.backend.adapters;

import com.cuentas.backend.domain.Asset;
import com.cuentas.backend.application.ports.driving.AssetServicePort;
import com.cuentas.backend.domain.AssetROI;
import com.cuentas.backend.domain.MonthlyROI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/{assetId}/roi")
    public ResponseEntity<AssetROI> calculateAssetRoi(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        logger.info("Calculando ROI para userId={}, assetId={}, startDate={}, endDate={}", userId, assetId, startDate, endDate);
        AssetROI roi = assetService.calculateAssetROI(userId, assetId, startDate, endDate);
        logger.info("Respuesta calculateAssetRoi: {}", roi);
        return ResponseEntity.ok(roi);
    }

    @GetMapping("/{assetId}/roi/monthly")
    public ResponseEntity<List<MonthlyROI>> getMonthlyRoi(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestParam(required = false) Integer year) {
        logger.info("Obteniendo ROI mensual para userId={}, assetId={}, year={}", userId, assetId, year);
        List<MonthlyROI> monthlyRoi = assetService.calculateMonthlyROI(userId, assetId, year);
        logger.info("Respuesta getMonthlyRoi: {}", monthlyRoi);
        return ResponseEntity.ok(monthlyRoi);
    }
}
