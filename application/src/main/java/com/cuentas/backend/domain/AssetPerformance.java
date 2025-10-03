package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPerformance {
    private Long assetId;
    private BigDecimal initialValue;
    private Double currentValue;
    private BigDecimal roi; // porcentaje

}