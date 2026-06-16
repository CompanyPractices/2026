package com.processing.transactionlogger.repository;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA-репозиторий транзакций с поддержкой Specification-фильтрации
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    long countByStatus(TransactionStatus status);

    /** @return суммарный объём всех транзакций в минорных единицах */
    @Query("SELECT SUM(t.amount) FROM Transaction t")
    long sumAmount();

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

    /**
     * Агрегирует транзакции по временным корзинам для графиков Dashboard.
     * Группировка выполняется по {@code created_at}, усечённому до заданного шага
     * через PostgreSQL {@code date_trunc}. Усечение выполняется в UTC (третий аргумент
     * {@code date_trunc}), поэтому границы корзин не зависят от часового
     * пояса сессии БД. Возвращаются только непустые корзины, упорядоченные по времени.
     *
     * @param interval шаг агрегации для {@code date_trunc}: {@code hour} или {@code day}
     * @param from     нижняя граница {@code created_at} (включительно)
     * @param to       верхняя граница {@code created_at} (исключительно)
     * @return строки агрегации корзин, упорядоченные по времени
     */
    @Query(value = """
        SELECT date_trunc(:interval, created_at, 'UTC') AS bucket,
            COUNT(*) AS total,
            COUNT(*) FILTER (WHERE status = 'APPROVED') AS approved,
            COUNT(*) FILTER (WHERE status = 'DECLINED') AS declined,
            COALESCE(SUM(amount), 0) AS amount
        FROM transactions
        WHERE created_at >= :from and created_at < :to
        GROUP BY bucket
        ORDER BY bucket
""", nativeQuery = true)
    List<ChartBucketRow> aggregateByInterval(@Param("interval") String interval,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);
}
