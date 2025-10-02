package com.cuentas.backend.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Liability {
    private Long liabilityId;
    private Long userId;
    private Long liabilityTypeId;
    private String name;
    private String description;
    private Double principalAmount;
    private Double interestRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double outstandingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Liability(String name, Long userId, Long liabilityTypeId, Double outstandingBalance){
        this.name = name;
        this.userId = userId;
        this.liabilityTypeId = liabilityTypeId;
        this.outstandingBalance = outstandingBalance;
    }

    public Liability() {

    }

    public Long getLiabilityId() {
        return liabilityId;
    }

    public void setLiabilityId(Long liabilityId) {
        this.liabilityId = liabilityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLiabilityTypeId() {
        return liabilityTypeId;
    }

    public void setLiabilityTypeId(Long liabilityTypeId) {
        this.liabilityTypeId = liabilityTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(Double principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Double getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(Double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
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
