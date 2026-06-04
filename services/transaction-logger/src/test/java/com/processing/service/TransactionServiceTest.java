package com.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.dto.TransactionRequest;
import com.processing.dto.TransactionStoredResponse;
import com.processing.enums.TransactionStatus;
import com.processing.mapper.TransactionMapper;
import com.processing.model.Transaction;
import com.processing.repository.TransactionRepository;
import com.processing.websocket.WebSocketManager;
import org.junit.jupiter.api.Test;
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

class TransactionServiceTest {

    @Test
    void storeReturnsExistingTransactionWhenIdAlreadyExists() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Transaction transaction = transaction(id);
        RepositoryFake repository = new RepositoryFake(transaction);
        CapturingWebSocketManager webSocketManager = new CapturingWebSocketManager();
        TransactionService transactionService = transactionService(repository, webSocketManager);

        TransactionStoreResult result = transactionService.store(transactionRequest());

        assertThat(result.created()).isFalse();
        assertThat(result.existingTransaction().id()).isEqualTo(id);
        assertThat(result.storedTransaction()).isNull();
        assertThat(repository.saveCount).isZero();
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

    private static Transaction transaction(UUID id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        return transaction;
    }

    private static class RepositoryFake implements InvocationHandler {
        private final Map<UUID, Transaction> transactions = new HashMap<>();
        private int saveCount;

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
            return Optional.ofNullable(transactions.get(id));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById" -> findById((UUID) args[0]);
                case "save" -> save((Transaction) args[0]);
                case "toString" -> "RepositoryFake";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private Transaction save(Transaction transaction) {
            saveCount++;
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
