package com.processing.dto;

import com.processing.model.Transaction;
import java.util.List;
import java.util.UUID;


public record TransactionSearchResponse(
        List<TransactionResponse> transactions,
        UUID nextPagingKey
) {}
