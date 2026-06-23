package com.processing.transactionlogger.service;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.dto.ChartBucket;
import com.processing.transactionlogger.model.Transaction;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.specification.ChartsFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DashboardChartsIntegrationTest {
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void getChartsGroupsTransactionsByHour() {
        save(at("2026-06-16T10:15:00Z", TransactionStatus.APPROVED, new BigDecimal("100000")));
        save(at("2026-06-16T10:45:00Z", TransactionStatus.DECLINED, new BigDecimal("50000")));
        save(at("2026-06-16T11:05:00Z", TransactionStatus.APPROVED, new BigDecimal("200000")));

        List<ChartBucket> charts = transactionService.getCharts(filter("hour", null, null));

        assertThat(charts).hasSize(2);

        ChartBucket firstHour = charts.get(0);
        assertEquals(Instant.parse("2026-06-16T10:00:00Z"), firstHour.timestamp());
        assertEquals(2, firstHour.total());
        assertEquals(1, firstHour.approved());
        assertEquals(1, firstHour.declined());
        assertEquals(new BigDecimal("150000"), firstHour.amount());

        ChartBucket secondHour = charts.get(1);
        assertEquals(Instant.parse("2026-06-16T11:00:00Z"), secondHour.timestamp());
        assertEquals(1, secondHour.total());
        assertEquals(new BigDecimal("200000"), secondHour.amount());
    }

    @Test
    void getChartsGroupsTransactionsByDay() {
        save(at("2026-06-16T10:00:00Z", TransactionStatus.APPROVED, new BigDecimal("100000")));
        save(at("2026-06-16T23:30:00Z", TransactionStatus.APPROVED, new BigDecimal("100000")));
        save(at("2026-06-17T01:00:00Z", TransactionStatus.DECLINED, new BigDecimal("50000")));

        List<ChartBucket> charts = transactionService.getCharts(filter("day", null, null));

        assertThat(charts).hasSize(2);
        assertEquals(Instant.parse("2026-06-16T00:00:00Z"), charts.get(0).timestamp());
        assertEquals(2, charts.get(0).total());
        assertEquals(Instant.parse("2026-06-17T00:00:00Z"), charts.get(1).timestamp());
        assertThat(charts.get(1).declined()).isEqualTo(1);
    }

    @Test
    void getChartsFiltersByDateRange() {
        save(at("2026-06-15T10:00:00Z", TransactionStatus.APPROVED, new BigDecimal("100000")));
        save(at("2026-06-16T10:00:00Z", TransactionStatus.APPROVED, new BigDecimal("100000")));
        save(at("2026-06-17T10:00:00Z", TransactionStatus.APPROVED, new BigDecimal("100000")));

        List<ChartBucket> charts = transactionService.getCharts(
                filter("day", Instant.parse("2026-06-16T00:00:00Z"), Instant.parse("2026-06-17T00:00:00Z")));

        assertThat(charts).hasSize(1);
        assertEquals(Instant.parse("2026-06-16T00:00:00Z"), charts.get(0).timestamp());
    }

    @Test
    void getChartsReturnsEmptyBucketWhenNoTransactions() {
        List<ChartBucket> charts = transactionService.getCharts(filter("hour", null, null));

        assertThat(charts).isEmpty();
    }

    private static ChartsFilter filter(String granularity, Instant from, Instant to) {
        ChartsFilter filter = new ChartsFilter();
        filter.setGranularity(granularity);
        filter.setFrom(from);
        filter.setTo(to);
        return filter;
    }
    private Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
    private Transaction at(String createdAt, TransactionStatus status, BigDecimal amount) {
        Instant instant = Instant.parse(createdAt);
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
        t.setTransmissionDateTime(instant);
        t.setCreatedAt(instant);
        return t;
    }

}
