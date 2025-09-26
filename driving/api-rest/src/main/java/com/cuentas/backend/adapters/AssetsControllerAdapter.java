package com.cuentas.backend.adapters;

import com.cuentas.backend.domain.Asset;
import com.cuentas.backend.application.ports.driving.AssetServicePort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/assets")
public class AssetsControllerAdapter {

    private final AssetServicePort assetService;

    public AssetsControllerAdapter(AssetServicePort assetService) {
        this.assetService = assetService;
    }

    @GetMapping()
    public ResponseEntity<List<Asset>> listAssets(@PathVariable Long userId) {
        return ResponseEntity.ok(assetService.listAssets(userId));
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<Asset> getAsset(@PathVariable Long userId, @PathVariable Long assetId) {
        return ResponseEntity.ok(assetService.getAsset(userId, assetId));
    }

    @PostMapping
    public ResponseEntity<Asset> createAsset(@PathVariable Long userId, @RequestBody Asset asset) {
        Asset created = assetService.createAsset(userId, asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<Asset> updateAsset(@PathVariable Long userId, @PathVariable Long assetId, @RequestBody Asset asset) {
        Asset updated = assetService.updateAsset(userId, assetId, asset);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long userId, @PathVariable Long assetId) {
        assetService.deleteAsset(userId, assetId);
        return ResponseEntity.noContent().build();
    }
}
