package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.FeeCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeeCalculateTest {
    private final FeeCalculator feeCalculator = new FeeCalculator();

    @Test
    void calculateFeeWithHalfEvenRounding() {
        BigDecimal fee = BigDecimal.valueOf(0.015);
        BigDecimal amount = BigDecimal.valueOf(1399_99);
        BigDecimal expected = amount.multiply(fee).setScale(0, RoundingMode.HALF_EVEN);

        assertEquals(expected, feeCalculator.calculate(fee, amount));
    }
}
