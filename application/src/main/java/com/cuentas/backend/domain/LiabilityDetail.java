package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityDetail {
    private Liability liability;
    private BigDecimal currentOutstandingBalance; // Saldo pendiente actual
    private BigDecimal principalPaid; // Capital pagado
    private BigDecimal progressPercentage; // Porcentaje de progreso
    private Integer transactionCount;
    private List<Transaction> recentTransactions; // Últimas 10 transacciones
    private List<LiabilityValue> valueHistory; // Historial de valores (últimos 12 meses)
    private List<Interest> interests; // Intereses asociados
}

