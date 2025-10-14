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
public class AssetValue {
    private Long assetValueId;
    private Long assetId;
    private LocalDate valuationDate;
    private Double currentValue;
}