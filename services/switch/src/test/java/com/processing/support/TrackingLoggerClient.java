package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.config.RetryFactory;
import com.processing.service.LoggerClient;

/**
 * Тестовый double {@link LoggerClient}, фиксирующий факт вызова и последнюю транзакцию.
 */
public class TrackingLoggerClient extends LoggerClient {

    private boolean called;
    private TransactionRequest lastTransaction;
    private final boolean succeed;

    /**
     * @param succeed {@code true} — {@link #log} возвращает успех; {@code false} — имитирует сбой Logger
     */
    public TrackingLoggerClient(boolean succeed) {
        super(
                SwitchTestData.defaultProperties(),
                null,
                RetryFactory.loggerRetry(SwitchTestData.defaultProperties()));
        this.succeed = succeed;
    }

    /** {@inheritDoc} */
    @Override
    public boolean log(TransactionRequest transaction) {
        called = true;
        lastTransaction = transaction;
        return succeed;
    }

    /**
     * @return {@code true}, если {@link #log} был вызван хотя бы раз
     */
    public boolean wasCalled() {
        return called;
    }

    /**
     * @return последняя транзакция, переданная в {@link #log}
     */
    public TransactionRequest lastTransaction() {
        return lastTransaction;
    }
}
