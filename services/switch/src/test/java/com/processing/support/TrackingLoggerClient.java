package com.processing.support;

import com.processing.SwitchTestData;
import com.processing.model.Transaction;
import com.processing.service.LoggerClient;

public class TrackingLoggerClient extends LoggerClient {

    private boolean called;
    private Transaction lastTransaction;
    private final boolean succeed;

    public TrackingLoggerClient(boolean succeed) {
        super(SwitchTestData.defaultProperties(), null);
        this.succeed = succeed;
    }

    @Override
    public boolean log(Transaction transaction) {
        called = true;
        lastTransaction = transaction;
        return succeed;
    }

    public boolean wasCalled() {
        return called;
    }

    public Transaction lastTransaction() {
        return lastTransaction;
    }
}
