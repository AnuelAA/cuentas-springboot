package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.TransactionServicePort;
import com.cuentas.backend.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/transactions")
public class TransactionsControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsControllerAdapter.class);

    private final TransactionServicePort transactionService;

    public TransactionsControllerAdapter(TransactionServicePort transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> listTransactions(@PathVariable Long userId,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                              @RequestParam(required = false) Long liabilityId,
                                                              @RequestParam(required = false) Long assetId,
                                                              @RequestParam(required = false) Long categoryId,
                                                              @RequestParam(required = false) Long relatedAssetId
                                                              ) {
        logger.info("Listando transacciones para userId={}, startDate={}, endDate={}, liabilityId={}, assetId={}, categoryId={}, relatedAssetId={}",
                userId, startDate, endDate, liabilityId, assetId, categoryId, relatedAssetId);
        List<Transaction> transactions = transactionService.listTransactions(userId, startDate, endDate, liabilityId, assetId, categoryId, relatedAssetId);
        logger.info("Respuesta listTransactions: {}", transactions);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long userId, @PathVariable Long transactionId) {
        logger.info("Obteniendo transacción con transactionId={} para userId={}", transactionId, userId);
        Transaction transaction = transactionService.getTransaction(userId, transactionId);
        logger.info("Respuesta getTransaction: {}", transaction);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@PathVariable Long userId, @RequestBody Transaction transaction) {
        logger.info("Creando transacción para userId={}, transaction={}", userId, transaction);
        Transaction created = transactionService.createTransaction(userId, transaction);
        logger.info("Respuesta createTransaction: {}", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long userId, @PathVariable Long transactionId, @RequestBody Transaction transaction) {
        logger.info("Actualizando transacción con transactionId={} para userId={}, transaction={}", transactionId, userId, transaction);
        Transaction updated = transactionService.updateTransaction(userId, transactionId, transaction);
        logger.info("Respuesta updateTransaction: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long userId, @PathVariable Long transactionId) {
        logger.info("Eliminando transacción con transactionId={} para userId={}", transactionId, userId);
        transactionService.deleteTransaction(userId, transactionId);
        logger.info("Respuesta deleteTransaction: No Content");
        return ResponseEntity.noContent().build();
    }
}
