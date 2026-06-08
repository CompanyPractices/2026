package com.processing.transactionlogger.dto;

public record DashboardStatsResponse(
    long totalTransactions,
    long approvedCount,
    long declinedCount,
    double approvalRate,
    long totalAmount,
    long averageAmount,
    double avgProcessingTimeMs,
    double transactionsPerMinute
) {}
