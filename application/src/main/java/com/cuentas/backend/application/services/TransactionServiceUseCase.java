package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.TransactionServicePort;
import com.cuentas.backend.domain.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class TransactionServiceUseCase implements TransactionServicePort {

    private final JdbcTemplate jdbcTemplate;

    public TransactionServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Transaction createTransaction(Long userId, Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, category_id, asset_id, liability_id, amount, transaction_date, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING transaction_id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                userId,
                transaction.getCategoryId(),
                transaction.getAssetId(),
                transaction.getLiabilityId(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription()
        );
        transaction.setTransactionId(id);
        transaction.setUserId(userId);
        return transaction;
    }

    @Override
    public Transaction getTransaction(Long userId, Long transactionId) {
        String sql = "SELECT * FROM transactions WHERE user_id = ? AND transaction_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), userId, transactionId);
    }

    @Override
    public List<Transaction> listTransactions(Long userId) {
        String sql = "SELECT * FROM transactions WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    @Override
    public Transaction updateTransaction(Long userId, Long transactionId, Transaction transaction) {
        String sql = "UPDATE transactions SET category_id = ?, asset_id = ?, liability_id = ?, amount = ?, transaction_date = ?, description = ?, updated_at = NOW() " +
                "WHERE user_id = ? AND transaction_id = ?";
        jdbcTemplate.update(sql,
                transaction.getCategoryId(),
                transaction.getAssetId(),
                transaction.getLiabilityId(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                userId,
                transactionId
        );
        return getTransaction(userId, transactionId);
    }

    @Override
    public void deleteTransaction(Long userId, Long transactionId) {
        String sql = "DELETE FROM transactions WHERE user_id = ? AND transaction_id = ?";
        jdbcTemplate.update(sql, userId, transactionId);
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("transaction_id"));
        t.setUserId(rs.getLong("user_id"));
        t.setCategoryId(rs.getObject("category_id") != null ? rs.getLong("category_id") : null);
        t.setAssetId(rs.getObject("asset_id") != null ? rs.getLong("asset_id") : null);
        t.setLiabilityId(rs.getObject("liability_id") != null ? rs.getLong("liability_id") : null);
        t.setAmount(rs.getBigDecimal("amount"));
        t.setTransactionDate(rs.getDate("transaction_date") != null ? rs.getDate("transaction_date").toLocalDate() : null);
        t.setDescription(rs.getString("description"));
        t.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        t.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return t;
    }
}
