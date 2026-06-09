package com.processing.transactionlogger.service;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DashboardServiceIntegrationTest {
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void getStatsReturnsZeroWhenDatabaseIsEmpty() {
        DashboardStatsResponse stats = transactionService.getStats();

        assertThat(stats.totalTransactions()).isZero();
        assertThat(stats.approvedCount()).isZero();
        assertThat(stats.declinedCount()).isZero();
        assertThat(stats.approvalRate()).isZero();
        assertThat(stats.totalAmount()).isZero();
        assertThat(stats.averageAmount()).isZero();
        assertThat(stats.avgProcessingTimeMs()).isZero();
    }

    @Test
    void getStatsReturnsCorrectCountsAndAmounts() {
        save(transaction(TransactionStatus.APPROVED, 100000L, 40));
        save(transaction(TransactionStatus.APPROVED, 200000L, 60));
        save(transaction(TransactionStatus.APPROVED, 300000L, 80));
        save(transaction(TransactionStatus.DECLINED, 50000L, 20));

        DashboardStatsResponse stats = transactionService.getStats();

        assertThat(stats.totalTransactions()).isEqualTo(4);
        assertThat(stats.approvedCount()).isEqualTo(3);
        assertThat(stats.declinedCount()).isEqualTo(1);
        assertThat(stats.totalAmount()).isEqualTo(650000L);
        assertThat(stats.averageAmount()).isEqualTo(162500L);
        assertThat(stats.avgProcessingTimeMs()).isEqualTo(50.0);
    }

    @Test
    void getStatsCalculatesApprovalRateCorrectly() {
        save(transaction(TransactionStatus.APPROVED, 100000L, 30));
        save(transaction(TransactionStatus.APPROVED, 100000L, 30));
        save(transaction(TransactionStatus.APPROVED, 100000L, 30));
        save(transaction(TransactionStatus.DECLINED, 100000L, 30));

        DashboardStatsResponse stats = transactionService.getStats();

        assertThat(stats.approvalRate()).isEqualTo(0.75);
    }

    @Test
    void getStatsCountsRecentTransactions() {
        save(transaction(TransactionStatus.APPROVED, 100000L, 30));

        Transaction old = transaction(TransactionStatus.APPROVED, 100000L, 30);
        old.setCreatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        save(old);

        DashboardStatsResponse stats = transactionService.getStats();

        assertThat(stats.transactionsPerMinute()).isEqualTo(1);
    }

    @Test
    void getRecentReturnsSortedByCreatedAtDesc() {
        Transaction first = transaction(TransactionStatus.APPROVED, 100000L, 30);
        first.setCreatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        save(first);

        Transaction second = transaction(TransactionStatus.APPROVED, 200000L, 30);
        second.setCreatedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        save(second);

        Transaction third = transaction(TransactionStatus.APPROVED, 300000L, 30);
        third.setCreatedAt(Instant.now());
        save(third);

        List<TransactionResponse> result = transactionService.getRecent(10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).amount()).isEqualTo(300000L);
        assertThat(result.get(2).amount()).isEqualTo(100000L);
    }

    private Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    private Transaction transaction(TransactionStatus status, long amount, int processingTimeMs) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setMti("0100");
        t.setStan("000001");
        t.setPan("4000000000000001");
        t.setProcessingCode("000000");
        t.setAmount(amount);
        t.setCurrencyCode("643");
        t.setTerminalId("TERM0001");
        t.setMerchantId("MERCHANT000001");
        t.setMcc("5411");
        t.setAcquirerId("ACQ001");
        t.setStatus(status);
        t.setProcessingTimeMs(processingTimeMs);
        t.setTransmissionDateTime(Instant.now());
        t.setCreatedAt(Instant.now());
        return t;
    }
}
