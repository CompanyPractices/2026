package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.service.LoggerClient;

public class TrackingLoggerClient extends LoggerClient {

    private boolean called;
    private TransactionRequest lastTransaction;
    private final boolean succeed;

    public TrackingLoggerClient(boolean succeed) {
        super(SwitchTestData.defaultProperties(), null);
        this.succeed = succeed;
    }

    @Override
    public boolean log(TransactionRequest transaction) {
        called = true;
        lastTransaction = transaction;
        return succeed;
    }

    public boolean wasCalled() {
        return called;
    }

    public TransactionRequest lastTransaction() {
        return lastTransaction;
    }
}
