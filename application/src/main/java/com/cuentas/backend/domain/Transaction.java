package com.cuentas.backend.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaction {
    private Long transactionId;
    private Long userId;
    private Long categoryId;
    private Long assetId;
    private Long liabilityId;
    private Double amount;
    private LocalDate transactionDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Transaction(Long userId, Long categoryId, Long assetId, Long liabilityId, Double amount, LocalDate transactionDate) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.assetId = assetId;
        this.liabilityId = liabilityId;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }
    public Transaction(){}

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getLiabilityId() {
        return liabilityId;
    }

    public void setLiabilityId(Long liabilityId) {
        this.liabilityId = liabilityId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
