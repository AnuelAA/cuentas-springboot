package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interest {
    private Long interestId;
    private Long liabilityId;
    private String type; // 'fixed', 'variable', 'general'
    private Double annualRate;
    private LocalDate startDate;
    private LocalDateTime createdAt;
}

