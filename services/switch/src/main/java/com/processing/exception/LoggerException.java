package com.processing.exception;

import java.util.UUID;

public class LoggerException extends RuntimeException {

    private final String stan;

    public LoggerException(String stan, UUID transactionId, int attempts) {
        super(String.format(
                "Logger unavailable for TX %s (id=%s) after %d attempts",
                stan, transactionId, attempts));
        this.stan = stan;
    }

    public String getStan() {
        return stan;
    }

}
