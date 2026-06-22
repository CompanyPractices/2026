package com.processing.transactionlogger.controller;

import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.transactionlogger.dto.ChartBucket;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.transactionlogger.service.TransactionService;
import com.processing.transactionlogger.specification.ChartsFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {
    @Mock
    private TransactionService transactionService;
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        dashboardController = new DashboardController(transactionService);
    }

    @Test
    void getStatsReturnsStatsFromService() {
        DashboardStatsResponse expected = stats(100L, 80L, 20L);
        when(transactionService.getStats()).thenReturn(expected);

        DashboardStatsResponse result = dashboardController.getStats();

        assertEquals(expected, result);
    }

    @Test
    void getRecentReturnsTransactionsFromService() {
        List<TransactionResponse> expected = List.of(transactionResponse());
        when(transactionService.getRecent(20)).thenReturn(expected);

        List<TransactionResponse> result = dashboardController.getRecent(20);

        assertEquals(expected, result);
    }

    @Test
    void getRecentPassesLimitToService() {
        when(transactionService.getRecent(5)).thenReturn(List.of());

        dashboardController.getRecent(5);

        verify(transactionService).getRecent(5);
    }

    @Test
    void getChartsReturnsResultFromService() {
        List<ChartBucket> expected = List.of(
                new ChartBucket(Instant.parse("2026-06-16T10:00:00Z"), 10, 7, 3, 500000));
        when(transactionService.getCharts(any())).thenReturn(expected);

        List<ChartBucket> result = dashboardController.getCharts(new ChartsFilter());

        assertEquals(expected, result);
    }
    @Test
    void getChartsPassesFilterToService() {
        when(transactionService.getCharts(any())).thenReturn(List.of());
        ChartsFilter filter = new ChartsFilter();
        filter.setGranularity("day");

        dashboardController.getCharts(filter);

        verify(transactionService).getCharts(filter);
    }

    private static DashboardStatsResponse stats(long total, long approved, long declined) {
        double rate = total > 0 ? (double) approved / total : 0;
        return new DashboardStatsResponse(total, approved, declined, rate, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0);
    }

    private static TransactionResponse transactionResponse() {
        return new TransactionResponse(
                UUID.randomUUID(),
                "0100",
                "000001",
                "012345678901",
                "4000001234560001",
                "000000",
                new BigDecimal("150000"),
                "643",
                "TERM001",
                "POS",
                "MERCH12345678901",
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
}
