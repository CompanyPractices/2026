package com.processing.transactionlogger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.exception.TransactionConflictException;
import com.processing.transactionlogger.mapper.TransactionMapper;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.websocket.WebSocketManager;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionServiceTest {

    @Test
    void storeReturnsExistingTransactionWhenIdAlreadyExists() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction(request);
        RepositoryFake repository = new RepositoryFake(transaction);
        CapturingWebSocketManager webSocketManager = new CapturingWebSocketManager();
        TransactionService transactionService = transactionService(repository, webSocketManager);

        TransactionStoreResult result = transactionService.store(request);

        assertThat(result.created()).isFalse();
        assertThat(result.existingTransaction().id()).isEqualTo(request.id());
        assertThat(result.storedTransaction()).isNull();
        assertThat(repository.saveCount).isZero();
        assertThat(webSocketManager.lastMessage).isNull();
    }

    @Test
    void storeThrowsConflictWhenExistingTransactionDiffersFromRequest() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction(request);
        transaction.setAmount(200000L);
        RepositoryFake repository = new RepositoryFake(transaction);
        CapturingWebSocketManager webSocketManager = new CapturingWebSocketManager();
        TransactionService transactionService = transactionService(repository, webSocketManager);

        assertThatThrownBy(() -> transactionService.store(request))
                .isInstanceOf(TransactionConflictException.class)
                .hasMessageContaining(request.id().toString());

        assertThat(repository.saveCount).isZero();
        assertThat(webSocketManager.lastMessage).isNull();
    }

    @Test
    void storeReturnsExistingTransactionWhenConcurrentRetryAlreadySavedIt() {
        TransactionRequest request = transactionRequest();
        RepositoryFake repository = new RepositoryFake(transaction(request));
        repository.hideExistingOnFirstFind();
        repository.failSaveWith(new DataIntegrityViolationException("duplicate id"));
        CapturingWebSocketManager webSocketManager = new CapturingWebSocketManager();
        TransactionService transactionService = transactionService(repository, webSocketManager);

        TransactionStoreResult result = transactionService.store(request);

        assertThat(result.created()).isFalse();
        assertThat(result.existingTransaction().id()).isEqualTo(request.id());
        assertThat(result.storedTransaction()).isNull();
        assertThat(repository.saveCount).isOne();
        assertThat(webSocketManager.lastMessage).isNull();
    }

    @Test
    void storeThrowsConflictWhenConcurrentRetrySavedDifferentTransaction() {
        TransactionRequest request = transactionRequest();
        Transaction transaction = transaction(request);
        transaction.setAmount(200000L);
        RepositoryFake repository = new RepositoryFake(transaction);
        repository.hideExistingOnFirstFind();
        repository.failSaveWith(new DataIntegrityViolationException("duplicate id"));
        CapturingWebSocketManager webSocketManager = new CapturingWebSocketManager();
        TransactionService transactionService = transactionService(repository, webSocketManager);

        assertThatThrownBy(() -> transactionService.store(request))
                .isInstanceOf(TransactionConflictException.class)
                .hasMessageContaining(request.id().toString());

        assertThat(repository.saveCount).isOne();
        assertThat(webSocketManager.lastMessage).isNull();
    }

    @Test
    void storeSavesTransactionAndBroadcastsIt() {
        TransactionRequest request = transactionRequest();
        RepositoryFake repository = new RepositoryFake();
        CapturingWebSocketManager webSocketManager = new CapturingWebSocketManager();
        TransactionService transactionService = transactionService(repository, webSocketManager);

        TransactionStoreResult result = transactionService.store(request);

        assertThat(result.created()).isTrue();
        assertThat(result.existingTransaction()).isNull();
        assertThat(result.storedTransaction().id()).isEqualTo(request.id());
        assertThat(result.storedTransaction().status()).isEqualTo("stored");
        assertThat(repository.findById(request.id())).isPresent();
        assertThat(repository.saveCount).isOne();
        assertThat(webSocketManager.lastMessage).contains(request.id().toString());
    }

    private static TransactionService transactionService(
            RepositoryFake repository,
            CapturingWebSocketManager webSocketManager
    ) {
        return new TransactionService(
                repository.proxy(),
                new TransactionMapper(),
                webSocketManager,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private static TransactionRequest transactionRequest() {
        return new TransactionRequest(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                150000L,
                "643",
                "TERM001",
                "POS",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                "ISS001",
                2250L,
                TransactionStatus.APPROVED,
                null,
                "ABC123",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }

    private static Transaction transaction(TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setId(request.id());
        transaction.setMti(request.mti());
        transaction.setStan(request.stan());
        transaction.setRrn(request.rrn());
        transaction.setPan(request.pan());
        transaction.setProcessingCode(request.processingCode());
        transaction.setAmount(request.amount());
        transaction.setCurrencyCode(request.currencyCode());
        transaction.setTerminalId(request.terminalId());
        transaction.setMerchantId(request.merchantId());
        transaction.setMcc(request.mcc());
        transaction.setAcquirerId(request.acquirerId());
        transaction.setIssuerId(request.issuerId());
        transaction.setAcquiringFee(request.acquiringFee());
        transaction.setStatus(request.status());
        transaction.setDeclineReason(request.declineReason());
        transaction.setAuthCode(request.authCode());
        transaction.setProcessingTimeMs(request.processingTimeMs());
        transaction.setTransmissionDateTime(request.transmissionDateTime());
        transaction.setCreatedAt(request.createdAt());
        return transaction;
    }

    private static class RepositoryFake implements InvocationHandler {
        private final Map<UUID, Transaction> transactions = new HashMap<>();
        private int saveCount;
        private int findByIdCount;
        private boolean hideExistingOnFirstFind;
        private RuntimeException saveException;

        RepositoryFake(Transaction... transactions) {
            for (Transaction transaction : transactions) {
                this.transactions.put(transaction.getId(), transaction);
            }
        }

        TransactionRepository proxy() {
            return (TransactionRepository) Proxy.newProxyInstance(
                    TransactionRepository.class.getClassLoader(),
                    new Class<?>[]{TransactionRepository.class},
                    this
            );
        }

        Optional<Transaction> findById(UUID id) {
            findByIdCount++;
            if (hideExistingOnFirstFind && findByIdCount == 1) {
                return Optional.empty();
            }
            return Optional.ofNullable(transactions.get(id));
        }

        void hideExistingOnFirstFind() {
            hideExistingOnFirstFind = true;
        }

        void failSaveWith(RuntimeException exception) {
            saveException = exception;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById" -> findById((UUID) args[0]);
                case "save", "saveAndFlush" -> save((Transaction) args[0]);
                case "toString" -> "RepositoryFake";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private Transaction save(Transaction transaction) {
            saveCount++;
            if (saveException != null) {
                throw saveException;
            }
            transactions.put(transaction.getId(), transaction);
            return transaction;
        }
    }

    private static class CapturingWebSocketManager implements WebSocketManager {
        private String lastMessage;

        @Override
        public void addSession(WebSocketSession session) {
        }

        @Override
        public void removeSession(WebSocketSession session) {
        }

        @Override
        public void broadcast(String message) {
            lastMessage = message;
        }
    }
}
