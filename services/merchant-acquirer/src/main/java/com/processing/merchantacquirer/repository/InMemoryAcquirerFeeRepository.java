package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryAcquirerFeeRepository implements AcquirerFeeRepository {
    private final Map<AcquirerFeeRequest, BigDecimal> acquirerFeeMap = new ConcurrentHashMap<>();

    @Override
    public void save(AcquirerFeeRequest key, BigDecimal value) {
        acquirerFeeMap.put(key, value);
        log.info("AcquirerFee saved: {}, {}", key, value);
    }

    @Override
    public BigDecimal get(AcquirerFeeRequest key) {
        BigDecimal value = acquirerFeeMap.get(key);
        log.info("Get from AcquirerFeeRepository : {}", key);
        if (value == null) {
            throw new NullPointerException("Local table with acquirerFee is empty. Try generate transactions");
        }
        return value;
    }
}
