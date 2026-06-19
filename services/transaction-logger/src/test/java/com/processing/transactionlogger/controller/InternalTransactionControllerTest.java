package com.processing.transactionlogger.controller;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.service.TransactionStoreResult;
import com.processing.transactionlogger.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalTransactionControllerTest {

    @Test
    void storeReturnsExistingTransactionWhenIdAlreadyExists() {
        TransactionRequest request = transactionRequest();
        TransactionResponse existingResponse = transactionResponse(request.id());
        StubTransactionService transactionService = new StubTransactionService(
                TransactionStoreResult.existing(existingResponse)
        );
        InternalTransactionController controller = new InternalTransactionController(transactionService);

        ResponseEntity<?> response = controller.store(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(existingResponse, response.getBody());
    }

    @Test
    void storeSavesTransactionWhenIdDoesNotExist() {
        TransactionRequest request = transactionRequest();
        TransactionStoredResponse storedResponse = new TransactionStoredResponse(request.id(), "stored");
        StubTransactionService transactionService = new StubTransactionService(
                TransactionStoreResult.created(storedResponse)
        );
        InternalTransactionController controller = new InternalTransactionController(transactionService);

        ResponseEntity<?> response = controller.store(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(storedResponse, response.getBody());
        assertTrue(transactionService.storeCalled);
    }

    private static TransactionRequest transactionRequest() {
        return new TransactionRequest(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                new BigDecimal("150000"),
                "643",
                "TERM001",
                "POS",
                "MERCH1234567890",
                "5411",
                "ACQ001",
                "ISS001",
                new BigDecimal("2250"),
                TransactionStatus.APPROVED,
                null,
                "ABC123",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }

    private static TransactionResponse transactionResponse(UUID id) {
        return new TransactionResponse(
                id,
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                new BigDecimal("150000"),
                "643",
                "TERM001",
                "POS",
                "MERCH1234567890",
                "5411",
                "ACQ001",
                "ISS001",
                new BigDecimal("2250"),
                TransactionStatus.APPROVED,
                null,
                "ABC123",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }

    private static class StubTransactionService extends TransactionService {
        private final TransactionStoreResult result;
        private boolean storeCalled;

        StubTransactionService(TransactionStoreResult result) {
            super(null, null, null, null, null);
            this.result = result;
        }

        @Override
        public TransactionStoreResult store(TransactionRequest request) {
            storeCalled = true;
            return result;
        }
    }
}
