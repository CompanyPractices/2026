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

    @Test
    void calculateOnePointFivePercent() {
        assertEquals(new BigDecimal("1500"), feeCalculator.calculate(new BigDecimal("0.015"), new BigDecimal("100000")));
    }

    @Test
    void calculateWithUpFraction() {
        assertEquals(new BigDecimal("2100"), feeCalculator.calculate(new BigDecimal("0.015"), new BigDecimal("139999")));
    }

    @Test
    void calculateWithHalfEven() {
        assertEquals(new BigDecimal("2"), feeCalculator.calculate(new BigDecimal("0.5"), new BigDecimal("5")));
        assertEquals(new BigDecimal("4"), feeCalculator.calculate(new BigDecimal("0.5"), new BigDecimal("7")));
    }

    @Test
    void calculateZeroAmount() {
        assertEquals(new BigDecimal("0"), feeCalculator.calculate(new BigDecimal("0.5"), BigDecimal.ZERO));
    }

    @Test
    void calculateLargeAmount() {
        assertEquals(new BigDecimal("3500000000"), feeCalculator.calculate(new BigDecimal("0.035"), new BigDecimal("100000000000")));
    }
}
