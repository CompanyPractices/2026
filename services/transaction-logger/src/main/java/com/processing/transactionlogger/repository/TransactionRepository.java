package com.processing.transactionlogger.repository;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA-репозиторий транзакций с поддержкой Specification-фильтрации
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    long countByStatus(TransactionStatus status);

    /** @return суммарный объём всех транзакций в минорных единицах */
    @Query("SELECT SUM(t.amount) FROM Transaction t")
    BigDecimal sumAmount();

    /**
     * Возвращает агрегированную статистику одним запросом.
     */
    @Query(value = """
            SELECT
                COUNT(*) AS total,
                COUNT(*) FILTER (WHERE status = 'APPROVED') AS approved,
                COUNT(*) FILTER (WHERE status = 'DECLINED') AS declined,
                COALESCE(SUM(amount), 0) AS total_amount,
                COUNT(*) FILTER (WHERE created_at > NOW() - INTERVAL '1 minute') AS recent_count,
                COALESCE(AVG(processing_time_ms), 0) AS avg_processing_time_ms
            FROM transactions
""", nativeQuery = true)
    TransactionStats findStats();
}
