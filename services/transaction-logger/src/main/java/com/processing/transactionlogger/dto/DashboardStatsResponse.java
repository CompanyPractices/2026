package com.processing.transactionlogger.dto;

import java.math.BigDecimal;

public record DashboardStatsResponse(
    long totalTransactions,
    long approvedCount,
    long declinedCount,
    double approvalRate,
    BigDecimal totalAmount,
    BigDecimal averageAmount,
    double avgProcessingTimeMs,
    double transactionsPerMinute
) {}
