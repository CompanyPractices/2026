package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAcquirerFeeRepository implements AcquirerFeeRepository {
    private final Map<AcquirerFeeRequest, Double> acquirerFeeMap = new ConcurrentHashMap<>();

    @Override
    public void save(AcquirerFeeRequest key, Double value) {
        acquirerFeeMap.put(key, value);
    }

    @Override
    public Double get(AcquirerFeeRequest key) {
        Double value = acquirerFeeMap.get(key);
        if (value == null) {
            throw new NullPointerException("Local table with acquirerFee is empty. Try generate transactions");
        }
        return value;
    }
}
