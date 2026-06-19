package com.processing.transactionlogger.controller;

import com.processing.transactionlogger.dto.ChartBucket;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.transactionlogger.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
public class DashboardControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void getStatsReturn200WithCorrectFields() throws Exception {
        when(transactionService.getStats()).thenReturn(
                new DashboardStatsResponse(
                        100L,
                        80L,
                        20L,
                        0.8,
                        new BigDecimal("500000"),
                        new BigDecimal("5000"),
                        42.5,
                        5.0)
        );

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(100))
                .andExpect(jsonPath("$.approvedCount").value(80))
                .andExpect(jsonPath("$.declinedCount").value(20))
                .andExpect(jsonPath("$.approvalRate").value(0.8))
                .andExpect(jsonPath("$.totalAmount").value(500000))
                .andExpect(jsonPath("$.averageAmount").value(5000))
                .andExpect(jsonPath("$.avgProcessingTimeMs").value(42.5))
                .andExpect(jsonPath("$.transactionsPerMinute").value(5.0));
    }

    @Test
    void getRecentReturns200WithDefaultLimit() throws Exception {
        when(transactionService.getRecent(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/recent"))
                .andExpect(status().isOk());
    }

    @Test
    void getRecentReturns400WhenLimitIsZero() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecentReturns400WhenLimitIsNegative() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent").param("limit", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecentReturns400WhenLimitExceedsMaximum() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent").param("limit", "501"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getChartsReturns200WithBuckets() throws Exception {
        when(transactionService.getCharts(any())).thenReturn(List.of(
                new ChartBucket(Instant.parse("2026-06-16T10:00:00Z"), 10, 7, 3, 500000)));

        mockMvc.perform(get("/api/dashboard/charts").param("granularity", "hour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].timestamp").value("2026-06-16T10:00:00Z"))
                .andExpect(jsonPath("$[0].total").value(10))
                .andExpect(jsonPath("$[0].approved").value(7))
                .andExpect(jsonPath("$[0].declined").value(3))
                .andExpect(jsonPath("$[0].amount").value(500000));
    }
    @Test
    void getChartsReturns200WithDefaultGranularity() throws Exception {
        when(transactionService.getCharts(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/charts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    @Test
    void getChartsReturns200WithDateTimeRange() throws Exception {
        when(transactionService.getCharts(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/charts")
                        .param("granularity", "day")
                        .param("from", "2026-06-16T00:00:00Z")
                        .param("to", "2026-06-16T23:59:59Z"))
                .andExpect(status().isOk());
    }
    @Test
    void getChartsReturns400WhenGranularityIsInvalid() throws Exception {
        mockMvc.perform(get("/api/dashboard/charts").param("granularity", "week"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void getChartsReturns400WhenFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/api/dashboard/charts")
                        .param("from", "2026-06-08T00:00:00Z")
                        .param("to", "2026-06-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

}
