package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.TransactionTemplate;

import java.util.List;

public interface TransactionTemplateServicePort {
    List<TransactionTemplate> getTransactionTemplates(Long userId);
    TransactionTemplate createTransactionTemplate(Long userId, TransactionTemplate template);
    TransactionTemplate updateTransactionTemplate(Long userId, Long templateId, TransactionTemplate template);
    void deleteTransactionTemplate(Long userId, Long templateId);
}

