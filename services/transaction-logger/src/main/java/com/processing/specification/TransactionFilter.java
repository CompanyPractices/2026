package com.processing.specification;

import com.processing.common.dto.annotations.ExactSize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@ValidDateRange
public class TransactionFilter {
    @ExactSize(16)
    private String pan;
    @Pattern(regexp = "APPROVED|DECLINED", message = "status must be APPROVED or DECLINED")
    private String status;
    private Instant from;
    private Instant to;
    private String merchantId;
    private String issuerId;
    @ExactSize(4)
    private String mcc;
    @Positive
    @Max(value = 100, message = "limit must not exceed 100")
    private int limit = 50;
    private UUID fromPagingKey;
}
