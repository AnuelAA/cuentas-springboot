package com.cuentas.backend.domain;

import java.time.LocalDateTime;

public class UserSettings {

    private Long settingId;
    private Long userId;
    private Boolean darkMode;
    private String language;
    private Boolean notificationsEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserSettings() {}

    public UserSettings(Long settingId, Long userId, Boolean darkMode, String language, Boolean notificationsEmail,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.settingId = settingId;
        this.userId = userId;
        this.darkMode = darkMode;
        this.language = language;
        this.notificationsEmail = notificationsEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters & Setters
    public Long getSettingId() { return settingId; }
    public void setSettingId(Long settingId) { this.settingId = settingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Boolean getDarkMode() { return darkMode; }
    public void setDarkMode(Boolean darkMode) { this.darkMode = darkMode; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Boolean getNotificationsEmail() { return notificationsEmail; }
    public void setNotificationsEmail(Boolean notificationsEmail) { this.notificationsEmail = notificationsEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
