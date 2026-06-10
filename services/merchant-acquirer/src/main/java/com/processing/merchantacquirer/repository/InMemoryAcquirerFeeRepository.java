package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryAcquirerFeeRepository implements AcquirerFeeRepository {
    private final Map<AcquirerFeeRequest, Double> acquirerFeeMap = new ConcurrentHashMap<>();

    @Override
    public void save(AcquirerFeeRequest key, Double value) {
        acquirerFeeMap.put(key, value);
        log.info("AcquirerFee saved: {}, {}", key, value);
    }

    @Override
    public Double get(AcquirerFeeRequest key) {
        Double value = acquirerFeeMap.get(key);
        log.info("Get from AcquirerFeeRepository : {}", key);
        if (value == null) {
            throw new NullPointerException("Local table with acquirerFee is empty. Try generate transactions");
        }
        return value;
    }
}
