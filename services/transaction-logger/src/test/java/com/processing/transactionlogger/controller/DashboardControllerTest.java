package com.processing.controller;

import com.processing.dto.DashboardStatsResponse;
import com.processing.dto.TransactionResponse;
import com.processing.enums.TransactionStatus;
import com.processing.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getRecentReturnsTransactionsFromService() {
        List<TransactionResponse> expected = List.of(transactionResponse());
        when(transactionService.getRecent(20)).thenReturn(expected);

        List<TransactionResponse> result = dashboardController.getRecent(20);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getRecentPassesLimitToService() {
        when(transactionService.getRecent(5)).thenReturn(List.of());

        dashboardController.getRecent(5);

        verify(transactionService).getRecent(5);
    }

    private static DashboardStatsResponse stats(long total, long approved, long declined) {
        double rate = total > 0 ? (double) approved / total : 0;
        return new DashboardStatsResponse(total, approved, declined, rate, 0L, 0L, 0.0, 0.0);
    }

    private static TransactionResponse transactionResponse() {
        return new TransactionResponse(
                UUID.randomUUID(),
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
}
