package com.processing.transactionlogger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.export.TransactionCsvWriter;
import com.processing.transactionlogger.mapper.TransactionMapper;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.repository.TransactionStats;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                new ObjectMapper().findAndRegisterModules(),
                new TransactionCsvWriter());
    }

    @Test
    void searchReturnsTransactionsMappedToDto() {
        Transaction transaction = Instancio.create(Transaction.class);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        TransactionSearchResponse result = transactionService.search(emptyFilter());

        assertThat(result.transactions()).hasSize(1);
        assertEquals(result.transactions().get(0).id(), transaction.getId());
    }

    @Test
    void searchReturnsTotalFromPage() {
        List<Transaction> content = Instancio.ofList(Transaction.class).size(3).create();
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content, Pageable.unpaged(), 150));

        TransactionSearchResponse result = transactionService.search(emptyFilter());

        assertEquals(150, result.total());
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
        when(transactionRepository.findStats())
                .thenReturn(new StatsStub(100L,
                        80L,
                        20L,
                        new BigDecimal("500000"),
                        5L,
                        42.5));

        DashboardStatsResponse stats = transactionService.getStats();

        assertEquals(100L, stats.totalTransactions());
        assertEquals(80L, stats.approvedCount());
        assertEquals(20L, stats.declinedCount());
        assertEquals(0.8, stats.approvalRate());
        assertThat(stats.totalAmount()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(stats.averageAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertEquals(42.5, stats.avgProcessingTimeMs());
        assertEquals(5.0, stats.transactionsPerMinute());
    }

    @Test
    void getStatsReturnsZeroWhenNotTransactions() {
        when(transactionRepository.findStats())
                .thenReturn(new StatsStub(0L,
                        0L,
                        0L,
                        BigDecimal.ZERO,
                        0L,
                        0.0));

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
        assertEquals(transactions.get(0).getId(), result.get(0).id());
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

    private record StatsStub(
            long total, long approved, long declined,
            BigDecimal totalAmount, long recentCount, double avgProcessingTimeMs
    ) implements TransactionStats {
        public long getTotal() { return total; }
        public long getApproved() { return approved; }
        public long getDeclined() { return declined; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public long getRecentCount() { return recentCount; }
        public double getAvgProcessingTimeMs() { return avgProcessingTimeMs; }
    }
}
