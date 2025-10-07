package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface TransactionServicePort {
    Transaction createTransaction(Long userId, Transaction transaction);
    Transaction getTransaction(Long userId, Long transactionId);
    List<Transaction> listTransactions(Long userId, LocalDate startDate, LocalDate endDate, Long liabilityId, Long assetId, Long categoryId, Long relatedAssetId);
    Transaction updateTransaction(Long userId, Long transactionId, Transaction transaction);
    void deleteTransaction(Long userId, Long transactionId);
}
