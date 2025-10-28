package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssetValueRequest {
    private LocalDate valuationDate;
    private Double currentValue;
    private Double acquisitionValue; // Opcional
}

