package com.processing.dto;

import com.processing.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public record TransactionSearchResponse {
    private long total;
    private List<Transaction> transactions;
}
