package com.processing.transactionlogger.repository;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
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
     * Считает транзакции, созданные после указанного момента.
     * Используется для расчёта {@code transactionsPerMinute} в статистике.
     *
     * @param since нижняя граница {@code createdAt} (не включается)
     * @return количество транзакций
     */
    long countByCreatedAtAfter(Instant since);

    /** @return среднее время обработки транзакции в миллисекундах */
    @Query("SELECT AVG(t.processingTimeMs) FROM Transaction t")
    double averageProcessingTimeMs();
}
