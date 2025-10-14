package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private Long assetId;
    private Long userId;
    private Long assetTypeId;
    private String name;
    private String description;
    private Double ownershipPercentage;
    private LocalDate acquisitionDate;
    private Double acquisitionValue;
    List<AssetValue> assetValues;

    public Asset(String name, Long userId,Long assetTypeId, LocalDate acquisitionDate, Double acquisitionValue){
        this.name = name;
        this.userId = userId;
        this.assetTypeId = assetTypeId;
        this.acquisitionDate = acquisitionDate;
        this.acquisitionValue = acquisitionValue;

    }
}