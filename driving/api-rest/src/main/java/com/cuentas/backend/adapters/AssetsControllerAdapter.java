package com.cuentas.backend.adapters;

import com.cuentas.backend.domain.Asset;
import com.cuentas.backend.domain.AssetDetail;
import com.cuentas.backend.domain.AssetValue;
import com.cuentas.backend.domain.CreateAssetValueRequest;
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

    @PostMapping("/{assetId}/valuations")
    public ResponseEntity<AssetValue> createAssetValuation(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestBody CreateAssetValueRequest request) {
        logger.info("Creando/actualizando valoración para userId={}, assetId={}, fecha={}", userId, assetId, request.getValuationDate());
        try {
            AssetValue assetValue = assetService.upsertAssetValue(
                    userId,
                    assetId,
                    request.getValuationDate(),
                    request.getCurrentValue(),
                    request.getAcquisitionValue()
            );
            logger.info("Valoración creada/actualizada: {}", assetValue);
            return ResponseEntity.ok(assetValue);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al crear/actualizar valoración: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{assetId}/valuations/{valuationId}")
    public ResponseEntity<AssetValue> updateAssetValuation(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @PathVariable Long valuationId,
            @RequestBody CreateAssetValueRequest request) {
        logger.info("Actualizando valoración para userId={}, assetId={}, valuationId={}", userId, assetId, valuationId);
        try {
            AssetValue assetValue = assetService.updateAssetValue(
                    userId,
                    assetId,
                    valuationId,
                    request.getValuationDate(),
                    request.getCurrentValue(),
                    request.getAcquisitionValue()
            );
            logger.info("Valoración actualizada: {}", assetValue);
            return ResponseEntity.ok(assetValue);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            logger.error("Error al actualizar valoración: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{assetId}/valuations/{valuationId}")
    public ResponseEntity<Void> deleteAssetValuation(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @PathVariable Long valuationId) {
        logger.info("Eliminando valoración para userId={}, assetId={}, valuationId={}", userId, assetId, valuationId);
        try {
            assetService.deleteAssetValue(userId, assetId, valuationId);
            logger.info("Valoración eliminada correctamente");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error al eliminar valoración: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{assetId}/detail")
    public ResponseEntity<AssetDetail> getAssetDetail(@PathVariable Long userId, @PathVariable Long assetId) {
        logger.info("Obteniendo detalle de assetId={} para userId={}", assetId, userId);
        try {
            AssetDetail detail = assetService.getAssetDetail(userId, assetId);
            logger.info("Respuesta getAssetDetail: valorActual={}, ingresos={}, gastos={}, ROI={}%", 
                    detail.getCurrentValue(), detail.getTotalIncome(), detail.getTotalExpenses(), detail.getRoiPercentage());
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            logger.error("Error al obtener detalle de activo: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/primary")
    public ResponseEntity<Asset> getPrimaryAsset(@PathVariable Long userId) {
        logger.info("Obteniendo activo principal para userId={}", userId);
        Asset primaryAsset = assetService.getPrimaryAsset(userId);
        if (primaryAsset == null) {
            logger.info("No hay activo principal para userId={}", userId);
            return ResponseEntity.notFound().build();
        }
        logger.info("Respuesta getPrimaryAsset: {}", primaryAsset);
        return ResponseEntity.ok(primaryAsset);
    }

    @PutMapping("/{assetId}/set-primary")
    public ResponseEntity<Asset> setPrimaryAsset(
            @PathVariable Long userId,
            @PathVariable Long assetId) {
        logger.info("Estableciendo activo principal para userId={}, assetId={}", userId, assetId);
        try {
            Asset updated = assetService.setPrimaryAsset(userId, assetId);
            logger.info("Activo principal establecido: {}", updated);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("Error al establecer activo principal: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
