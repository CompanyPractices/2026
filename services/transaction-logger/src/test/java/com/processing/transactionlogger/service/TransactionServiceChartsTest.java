package com.processing.transactionlogger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.transactionlogger.dto.ChartBucket;
import com.processing.transactionlogger.export.TransactionCsvWriter;
import com.processing.transactionlogger.mapper.TransactionMapper;
import com.processing.transactionlogger.repository.ChartBucketRow;
import com.processing.transactionlogger.repository.TransactionRepository;
import com.processing.transactionlogger.specification.ChartsFilter;
import com.processing.transactionlogger.websocket.WebSocketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceChartsTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WebSocketManager webSocketManager;
    @Captor
    private ArgumentCaptor<Instant> fromCaptor;
    @Captor
    private ArgumentCaptor<Instant> toCaptor;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                new TransactionMapper(),
                webSocketManager,
                new ObjectMapper().findAndRegisterModules(),
                new TransactionCsvWriter()
        );
    }

    @Test
    void getChartsMapsRowsToBuckets() {
        Instant bucket = Instant.parse("2026-06-16T10:00:00Z");
        ChartBucketRow row = bucketRow(bucket, 10, 7, 3, 500000);
        when(transactionRepository.aggregateByInterval(anyString(), any(), any()))
                .thenReturn(List.of(row));

        List<ChartBucket> buckets = transactionService.getCharts(filter(null, null));

        assertThat(buckets).containsExactly(new ChartBucket(bucket, 10, 7, 3, 500000));
    }

    @Test
    void getChartsUsesEpochToFarFutureWhenRangeMissing() {
        when(transactionRepository.aggregateByInterval(anyString(), fromCaptor.capture(), toCaptor.capture()))
                .thenReturn(List.of());

        transactionService.getCharts(filter(null, null));

        assertEquals(Instant.EPOCH, fromCaptor.getValue());
        assertEquals(Instant.parse("9999-12-31T23:59:59Z"), toCaptor.getValue());
    }

    @Test
    void getChartsReturnsEmptyWhenNoRows() {
        when(transactionRepository.aggregateByInterval(anyString(), any(), any()))
                .thenReturn(List.of());

        List<ChartBucket> buckets = transactionService.getCharts(filter(null, null));

        assertThat(buckets).isEmpty();
    }

    private static ChartBucketRow bucketRow(Instant bucket, long total, long approved,
                                            long declined, long amount) {
        ChartBucketRow row = mock(ChartBucketRow.class);
        when(row.getBucket()).thenReturn(bucket);
        when(row.getTotal()).thenReturn(total);
        when(row.getApproved()).thenReturn(approved);
        when(row.getDeclined()).thenReturn(declined);
        when(row.getAmount()).thenReturn(amount);
        return row;
    }

    private static ChartsFilter filter(Instant from, Instant to) {
        ChartsFilter filter = new ChartsFilter();
        filter.setFrom(from);
        filter.setTo(to);
        return filter;
    }
}
