package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReassignTransactionsRequest {
    private Long toCategoryId; // ID de la categoría destino
}

