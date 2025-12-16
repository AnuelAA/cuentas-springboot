package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTemplate {
    private Long templateId;
    private Long userId;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String type; // "income" or "expense"
    private Double amount;
    private Long assetId;
    private Long relatedAssetId;
    private Long liabilityId;
    private String description;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}

