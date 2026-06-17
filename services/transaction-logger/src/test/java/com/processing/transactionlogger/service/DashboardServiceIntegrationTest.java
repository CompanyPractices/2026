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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(0, stats.totalTransactions());
        assertEquals(0, stats.approvedCount());
        assertEquals(0, stats.declinedCount());
        assertEquals(0, stats.approvalRate());
        assertEquals(BigDecimal.ZERO, stats.totalAmount());
        assertEquals(BigDecimal.ZERO, stats.averageAmount());
        assertEquals(0, stats.avgProcessingTimeMs());
    }

    @Test
    void getStatsReturnsCorrectCountsAndAmounts() {
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 40));
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("200000"), 60));
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("300000"), 80));
        save(transaction(TransactionStatus.DECLINED, new BigDecimal("50000"), 20));

        DashboardStatsResponse stats = transactionService.getStats();

        assertEquals(4, stats.totalTransactions());
        assertEquals(3, stats.approvedCount());
        assertEquals(1, stats.declinedCount());
        assertThat(stats.totalAmount()).isEqualByComparingTo(new BigDecimal("650000"));
        assertThat(stats.averageAmount()).isEqualByComparingTo(new BigDecimal("162500"));
        assertEquals(50.0, stats.avgProcessingTimeMs());
    }

    @Test
    void getStatsCalculatesApprovalRateCorrectly() {
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 30));
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 30));
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 30));
        save(transaction(TransactionStatus.DECLINED, new BigDecimal("100000"), 30));

        DashboardStatsResponse stats = transactionService.getStats();

        assertEquals(0.75, stats.approvalRate());
    }

    @Test
    void getStatsCountsRecentTransactions() {
        save(transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 30));

        Transaction old = transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 30);
        old.setCreatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        save(old);

        DashboardStatsResponse stats = transactionService.getStats();

        assertEquals(1, stats.transactionsPerMinute());
    }

    @Test
    void getRecentReturnsSortedByCreatedAtDesc() {
        Transaction first = transaction(TransactionStatus.APPROVED, new BigDecimal("100000"), 30);
        first.setCreatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        save(first);

        Transaction second = transaction(TransactionStatus.APPROVED, new BigDecimal("200000"), 30);
        second.setCreatedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        save(second);

        Transaction third = transaction(TransactionStatus.APPROVED, new BigDecimal("300000"), 30);
        third.setCreatedAt(Instant.now());
        save(third);

        List<TransactionResponse> result = transactionService.getRecent(10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.get(2).amount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    private Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    private Transaction transaction(TransactionStatus status, BigDecimal amount, int processingTimeMs) {
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
