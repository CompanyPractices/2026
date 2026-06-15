package com.processing.transactionlogger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.exception.TransactionConflictException;
import com.processing.transactionlogger.mapper.TransactionMapper;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.model.Transaction_;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.specification.OffsetBasedPageRequest;
import com.processing.transactionlogger.specification.TransactionFilter;
import com.processing.transactionlogger.specification.TransactionSpecification;
import com.processing.transactionlogger.websocket.WebSocketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Сервис логирования транзакций.
 * Обеспечивает идемпотентное сохранение, поиск с фильтрацией,
 * агрегированную статистику и WebSocket-рассылку новых транзакций.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final WebSocketManager webSocketManager;
    private final ObjectMapper objectMapper;

    /**
     * Сохраняет транзакцию в БД и рассылает её через WebSocket.
     * Идемпотентен: повторный запрос с тем же {@code id} вернёт существующую запись.
     *
     * @param request данные транзакции от Switch
     * @return {@link TransactionStoreResult} с флагом {@code created=true} если запись новая,
     *         {@code created=false} если уже существовала
     * @throws TransactionConflictException если {@code id} занят транзакцией с другими данными
     */
    public TransactionStoreResult store(TransactionRequest request) {
        Optional<Transaction> existingTransaction = transactionRepository.findById(request.id());
        if (existingTransaction.isPresent()) {
            return existingTransactionResult(existingTransaction.get(), request);
        }

        Transaction savedTransaction;
        try {
            savedTransaction = transactionRepository.saveAndFlush(transactionMapper.toEntity(request));
        } catch (DataIntegrityViolationException exception) {
            log.warn("Data integrity violation while storing transaction: id={}",
                    request.id(),
                    exception);
            return transactionRepository.findById(request.id())
                    .map(transaction -> existingTransactionResult(transaction, request))
                    .orElseThrow(() -> exception);
        }

        try {
            TransactionResponse response = transactionMapper.toResponse(savedTransaction);
            webSocketManager.broadcast(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize transaction for WebSocket broadcast: {}", savedTransaction.getId(), exception);
        }

        return TransactionStoreResult.created(transactionMapper.toStoredResponse(savedTransaction));
    }

    private TransactionStoreResult existingTransactionResult(Transaction transaction, TransactionRequest request) {
        if (!transactionMapper.matches(transaction, request)) {
            TransactionConflictException exception = new TransactionConflictException(request.id());
            log.warn("Transaction conflict: existing transaction does not match request, id={}",
                    request.id(),
                    exception);
            throw exception;
        }

        TransactionResponse response = transactionMapper.toResponse(transaction);
        return TransactionStoreResult.existing(response);
    }

    /**
     * Ищет транзакции по фильтрам с пагинацией.
     * Все параметры фильтра опциональны и объединяются через AND.
     *
     * @param filter параметры фильтрации и пагинации
     * @return постраничный результат с общим счётчиком
     */
    public TransactionSearchResponse search(TransactionFilter filter) {
        Pageable pageable = new OffsetBasedPageRequest(filter.getOffset(), filter.getLimit());
        Page<Transaction> page = transactionRepository.findAll(TransactionSpecification.filter(filter), pageable);
        List<TransactionResponse> responses = page.getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();
        return new TransactionSearchResponse(page.getTotalElements(), responses);
    }

    /**
     * Возвращает агрегированную статистику по всем транзакциям в БД
     *
     * @return статистика: счётчики, суммы, процент одобрения, транзакций в минуту
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long total = transactionRepository.count();
        long approved = transactionRepository.countByStatus(TransactionStatus.APPROVED);
        long declined = transactionRepository.countByStatus(TransactionStatus.DECLINED);
        long totalAmount = total > 0 ? transactionRepository.sumAmount() : 0;
        long recentCount = transactionRepository.countByCreatedAtAfter(Instant.now().minusSeconds(60));
        double avgProcessingTimeMs = total > 0 ? transactionRepository.averageProcessingTimeMs() : 0;
        return new DashboardStatsResponse(
                total,
                approved,
                declined,
                total > 0 ? (double) approved / total : 0,
                totalAmount,
                total > 0 ? totalAmount / total : 0,
                avgProcessingTimeMs,
                recentCount
        );
    }

    /**
     * Возвращает последние транзакции, отсортированные по {@code createdAt DESC}
     *
     * @param limit максимальное число записей (1–500)
     * @return список транзакций
     */
    public List<TransactionResponse> getRecent(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, Transaction_.CREATED_AT));
        return transactionRepository.findAll(pageable).getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();
    }
}
