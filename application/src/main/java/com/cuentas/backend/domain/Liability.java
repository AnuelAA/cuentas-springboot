package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Liability {
    private Long liabilityId;
    private Long userId;
    private Long liabilityTypeId;
    private String name;
    private String description;
    private Double principalAmount;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double outstandingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Liability(String name, Long userId, Long liabilityTypeId, Double principalAmount, BigDecimal interestRate,
                     LocalDate startDate, LocalDate endDate, Double outstandingBalance){
        this.name = name;
        this.userId = userId;
        this.liabilityTypeId = liabilityTypeId;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.outstandingBalance = outstandingBalance;
    }
}