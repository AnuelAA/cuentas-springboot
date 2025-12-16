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
public class Budget {
    private Long budgetId;
    private Long userId;
    private Long categoryId;
    private Double amount;
    private String period; // "monthly" or "yearly"
    private LocalDate startDate;
    private LocalDate endDate;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}

