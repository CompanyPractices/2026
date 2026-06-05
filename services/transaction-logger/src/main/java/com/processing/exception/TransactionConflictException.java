package com.processing.exception;

import java.util.UUID;

public class TransactionConflictException extends RuntimeException {

    public TransactionConflictException(UUID id) {
        super("Transaction with id " + id + " already exists with different data");
    }
}
