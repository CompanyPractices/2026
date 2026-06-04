package com.processing.specification;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionFilter {
    private String pan;
    private String status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String merchantId;
    private String issuerId;
    private String mcc;
    @Positive
    private int limit = 50;
    @PositiveOrZero
    private int offset = 0;
}
