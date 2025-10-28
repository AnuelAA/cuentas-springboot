package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLiabilityValueRequest {
    private LocalDate valuationDate;
    private Double outstandingBalance;
    private LocalDate endDate; // Opcional
}

