package com.processing.dto;

import com.processing.common.dto.transactionlogger.TransactionResponse;

import java.util.List;


public record TransactionSearchResponse(
        long total,
        List<TransactionResponse> transactions
) {}
