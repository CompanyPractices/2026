package com.processing.dto;

import com.processing.model.Transaction;
import java.util.List;


public record TransactionSearchResponse(
        long total,
        List<Transaction> transactions
) {}
