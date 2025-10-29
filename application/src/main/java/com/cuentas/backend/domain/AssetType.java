package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetType {
    private Long assetTypeId;
    private String name;
    private String description;
    private Boolean isCheckingAccount;
}

