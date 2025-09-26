package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Asset;
import java.util.List;

public interface AssetServicePort {
    Asset createAsset(Long userId, Asset asset);
    Asset getAsset(Long userId, Long assetId);
    List<Asset> listAssets(Long userId);
    Asset updateAsset(Long userId, Long assetId, Asset asset);
    void deleteAsset(Long userId, Long assetId);
}
