package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;

import java.math.BigDecimal;

public interface AcquirerFeeRepository {
    void save(AcquirerFeeRequest key, BigDecimal amount);
    BigDecimal get(AcquirerFeeRequest key);
}
