package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

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
    private LocalDate acquisitionDate;
    private Double acquisitionValue;
    private Double currentValue;

    public Asset(String name, Long userId,Long assetTypeId, LocalDate acquisitionDate, Double acquisitionValue, Double currentValue){
        this.name = name;
        this.userId = userId;
        this.assetTypeId = assetTypeId;
        this.acquisitionDate = acquisitionDate;
        this.acquisitionValue = acquisitionValue;
        this.currentValue = currentValue;
    }
}