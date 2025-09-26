package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.TransactionServicePort;
import com.cuentas.backend.domain.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/transactions")
public class TransactionsControllerAdapter {

    private final TransactionServicePort transactionService;

    public TransactionsControllerAdapter(TransactionServicePort transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> listTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.listTransactions(userId));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long userId, @PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransaction(userId, transactionId));
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@PathVariable Long userId, @RequestBody Transaction transaction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(userId, transaction));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long userId, @PathVariable Long transactionId, @RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.updateTransaction(userId, transactionId, transaction));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long userId, @PathVariable Long transactionId) {
        transactionService.deleteTransaction(userId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
