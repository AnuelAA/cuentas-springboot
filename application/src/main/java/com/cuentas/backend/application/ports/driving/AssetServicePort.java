package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Asset;
import com.cuentas.backend.domain.AssetROI;
import com.cuentas.backend.domain.MonthlyROI;

import java.time.LocalDate;
import java.util.List;

public interface AssetServicePort {
    Asset createAsset(Long userId, Asset asset);
    Asset getAsset(Long userId, Long assetId);
    List<Asset> listAssets(Long userId);
    Asset updateAsset(Long userId, Long assetId, Asset asset);
    void deleteAsset(Long userId, Long assetId);
    AssetROI calculateAssetROI(Long userId, Long assetId, LocalDate startDate, LocalDate endDate);
    List<MonthlyROI> calculateMonthlyROI(Long userId, Long assetId, Integer year);
}
