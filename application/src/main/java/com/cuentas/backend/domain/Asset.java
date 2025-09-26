package com.cuentas.backend.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Asset {
    private Long assetId;
    private Long userId;
    private Long assetTypeId;
    private String name;
    private String description;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionValue;
    private BigDecimal currentValue;

    // Getters y Setters
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAssetTypeId() { return assetTypeId; }
    public void setAssetTypeId(Long assetTypeId) { this.assetTypeId = assetTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }
    public BigDecimal getAcquisitionValue() { return acquisitionValue; }
    public void setAcquisitionValue(BigDecimal acquisitionValue) { this.acquisitionValue = acquisitionValue; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
}