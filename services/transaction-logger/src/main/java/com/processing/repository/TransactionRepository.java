package com.processing.repository;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    long countByStatus(TransactionStatus status);
    @Query("SELECT SUM(t.amount) FROM Transaction t")
    long sumAmount();
    long countByCreatedAtAfter(Instant since);
    @Query("SELECT AVG(t.processingTimeMs) FROM  Transaction t")
    double averageProcessingTimeMs();
}
