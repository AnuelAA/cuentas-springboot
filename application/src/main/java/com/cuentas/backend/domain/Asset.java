package com.cuentas.backend.domain;


import java.time.LocalDate;

public class Asset {
    private Long assetId;
    private Long userId;
    private Long assetTypeId;
    private String name;
    private String description;
    private LocalDate acquisitionDate;
    private Double acquisitionValue;
    private Double currentValue;

    public Asset(String name, Long userId,Long assetId, Double currentValue){
        this.name = name;
        this.userId = userId;
        this.assetId = assetId;
        this.currentValue = currentValue;
    }
    public Asset() {}
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
    public Double getAcquisitionValue() { return acquisitionValue; }
    public void setAcquisitionValue(Double acquisitionValue) { this.acquisitionValue = acquisitionValue; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
}