package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.AssetType;
import java.util.List;

public interface AssetTypeServicePort {
    List<AssetType> getAllAssetTypes();
}

