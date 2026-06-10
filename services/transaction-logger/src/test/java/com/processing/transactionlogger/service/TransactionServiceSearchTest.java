package com.processing.transactionlogger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.mapper.TransactionMapper;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.specification.TransactionFilter;
import com.processing.transactionlogger.websocket.WebSocketManager;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceSearchTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WebSocketManager webSocketManager;
    private TransactionService transactionService;

    @BeforeEach()
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                new TransactionMapper(),
                webSocketManager,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void searchReturnsTransactionsMappedToDto() {
        Transaction transaction = Instancio.create(Transaction.class);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        TransactionSearchResponse result = transactionService.search(emptyFilter());

        assertThat(result.transactions()).hasSize(1);
        assertThat(result.transactions().get(0).id()).isEqualTo(transaction.getId());
    }

    @Test
    void searchReturnsTotalFromPage() {
        List<Transaction> content = Instancio.ofList(Transaction.class).size(3).create();
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content, Pageable.unpaged(), 150));

        TransactionSearchResponse result = transactionService.search(emptyFilter());

        assertThat(result.total()).isEqualTo(150);
        assertThat(result.transactions()).hasSize(3);
    }

    @Test
    void searchReturnsEmptyResultWhenNoTransactions() {
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TransactionSearchResponse result = transactionService.search(emptyFilter());

        assertThat(result.total()).isZero();
        assertThat(result.transactions()).isEmpty();
    }

    @Test
    void getStatsReturnsAggregatedData() {
        when(transactionRepository.count()).thenReturn(100L);
        when(transactionRepository.countByStatus(TransactionStatus.APPROVED)).thenReturn(80L);
        when(transactionRepository.countByStatus(TransactionStatus.DECLINED)).thenReturn(20L);
        when(transactionRepository.sumAmount()).thenReturn(500000L);
        when(transactionRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(5L);
        when(transactionRepository.averageProcessingTimeMs()).thenReturn(42.5);

        DashboardStatsResponse stats = transactionService.getStats();

        assertThat(stats.totalTransactions()).isEqualTo(100L);
        assertThat(stats.approvedCount()).isEqualTo(80L);
        assertThat(stats.declinedCount()).isEqualTo(20L);
        assertThat(stats.approvalRate()).isEqualTo(0.8);
        assertThat(stats.totalAmount()).isEqualTo(500000L);
        assertThat(stats.averageAmount()).isEqualTo(5000L);
        assertThat(stats.avgProcessingTimeMs()).isEqualTo(42.5);
        assertThat(stats.transactionsPerMinute()).isEqualTo(5.0);
    }

    @Test
    void getStatsReturnsZeroWhenNotTransactions() {
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionRepository.countByStatus(any())).thenReturn(0L);
        when(transactionRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);

        DashboardStatsResponse stats = transactionService.getStats();

        assertThat(stats.totalTransactions()).isZero();
        assertThat(stats.approvalRate()).isZero();
        assertThat(stats.totalAmount()).isZero();
        assertThat(stats.averageAmount()).isZero();
        assertThat(stats.avgProcessingTimeMs()).isZero();
    }

    @Test
    void getRecentReturnsTransactionsMappedToDto() {
        List<Transaction> transactions = Instancio.ofList(Transaction.class).size(5).create();
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(transactions));

        List<TransactionResponse> result = transactionService.getRecent(5);

        assertThat(result).hasSize(5);
        assertThat(result.get(0).id()).isEqualTo(transactions.get(0).getId());
    }

    @Test
    void getRecentReturnsEmptyListWhenNoTransactions() {
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<TransactionResponse> result = transactionService.getRecent(10);

        assertThat(result).isEmpty();
    }

    private static TransactionFilter emptyFilter() {
        return new TransactionFilter();
    }

}
