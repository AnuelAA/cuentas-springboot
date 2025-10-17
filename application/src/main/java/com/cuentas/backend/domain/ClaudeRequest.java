package com.cuentas.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaudeRequest {
    private String model;
    private List<ClaudeMessage> messages;
    private int max_tokens;
}