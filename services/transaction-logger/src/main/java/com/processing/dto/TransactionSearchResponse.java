package com.processing.dto;

import java.util.List;


public record TransactionSearchResponse(
        long total,
        List<TransactionResponse> transactions
) {}
