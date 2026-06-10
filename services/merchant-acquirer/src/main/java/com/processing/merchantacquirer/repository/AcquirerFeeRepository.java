package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;

public interface AcquirerFeeRepository {
    void save(AcquirerFeeRequest key, Double amount);
    Double get(AcquirerFeeRequest key);
}
