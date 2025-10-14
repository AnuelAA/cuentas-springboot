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
public class Transaction {
    private Long transactionId;
    private Long userId;
    private Long categoryId;
    private Long assetId;
    private Long relatedAssetId;
    private Long liabilityId;
    private Double amount;
    private String type; // "income" or "expense"
    private LocalDate transactionDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Transaction(Long userId, Long categoryId, Long assetId, Long liabilityId, Long relatedAssetId, Double amount, String type, LocalDate transactionDate) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.assetId = assetId;
        this.liabilityId = liabilityId;
        this.relatedAssetId = relatedAssetId;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }
}