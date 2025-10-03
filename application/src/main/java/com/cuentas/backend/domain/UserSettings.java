package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    private Long settingId;
    private Long userId;
    private Boolean darkMode;
    private String language;
    private Boolean notificationsEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}