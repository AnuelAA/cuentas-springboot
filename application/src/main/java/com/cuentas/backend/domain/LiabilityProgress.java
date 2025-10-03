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
public class LiabilityProgress {
    private Long liabilityId;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal remainingBalance;
    private BigDecimal progressPercentage;
}