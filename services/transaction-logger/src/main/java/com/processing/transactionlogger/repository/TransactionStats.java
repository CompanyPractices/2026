package com.processing.transactionlogger.repository;

/**
 * Результат агрегирующего запроса {@link TransactionRepository#findStats}.
 * Должен быть интерфейсом: Spring Data создаёт JDK-прокси,
 * который маппит SQL-алиасы (например {@code total_amount}) на геттеры ({@code getTotalAmount}).
 */
public interface TransactionStats {
    long getTotal();
    long getApproved();
    long getDeclined();
    long getTotalAmount();
    long getRecentCount();
    double getAvgProcessingTimeMs();
}
