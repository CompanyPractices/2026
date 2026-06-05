package com.processing.service;

import com.processing.dto.TransactionResponse;
import com.processing.dto.TransactionStoredResponse;

public record TransactionStoreResult(
        TransactionResponse existingTransaction,
        TransactionStoredResponse storedTransaction
) {
    public static TransactionStoreResult existing(TransactionResponse response) {
        return new TransactionStoreResult(response, null);
    }

    public static TransactionStoreResult created(TransactionStoredResponse response) {
        return new TransactionStoreResult(null, response);
    }

    public boolean created() {
        return storedTransaction != null;
    }
}
