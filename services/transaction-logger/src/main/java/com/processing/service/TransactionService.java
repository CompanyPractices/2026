package com.processing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.dto.DashboardStatsResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.dto.TransactionSearchResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.exception.TransactionConflictException;
import com.processing.mapper.TransactionMapper;
import com.processing.model.Transaction;
import com.processing.model.Transaction_;
import com.processing.repository.TransactionRepository;
import com.processing.specification.OffsetBasedPageRequest;
import com.processing.specification.TransactionFilter;
import com.processing.specification.TransactionSpecification;
import com.processing.websocket.WebSocketManager;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final WebSocketManager webSocketManager;
    private final ObjectMapper objectMapper;

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

    public TransactionSearchResponse search(TransactionFilter filter) {
        Pageable pageable = new OffsetBasedPageRequest(filter.getOffset(), filter.getLimit());
        Page<Transaction> page = transactionRepository.findAll(TransactionSpecification.filter(filter), pageable);
        List<TransactionResponse> responses = page.getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();
        return new TransactionSearchResponse(page.getTotalElements(), responses);
    }

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

    public List<TransactionResponse> getRecent(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, Transaction_.CREATED_AT));
        return transactionRepository.findAll(pageable).getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();
    }
}
