package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private Long interestId;
    private LocalDate startDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LiabilityValue> liabilityValues;

    public Liability(String name, Long userId, Long liabilityTypeId, Double principalAmount, LocalDate startDate){
        this.name = name;
        this.userId = userId;
        this.liabilityTypeId = liabilityTypeId;
        this.principalAmount = principalAmount;
        this.startDate = startDate;
    }
}