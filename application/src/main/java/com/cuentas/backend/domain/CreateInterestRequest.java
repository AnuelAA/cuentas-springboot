package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterestRequest {
    private String type; // 'fixed', 'variable', 'general' (opcional, default: 'fixed')
    private Double annualRate; // Tasa anual como decimal (ej: 2.5 = 2.5%) - opcional
    private LocalDate startDate; // Fecha de inicio (obligatorio)
}

