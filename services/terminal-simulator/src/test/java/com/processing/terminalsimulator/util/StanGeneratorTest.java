package com.processing.terminalsimulator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StanGeneratorTest {

    private StanGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StanGenerator();
    }

    @Test
    void shouldGenerateSequentialNumbersWithLeadingZeros() {
        assertEquals("000001", generator.getNextStan());
        assertEquals("000002", generator.getNextStan());
        assertEquals("000003", generator.getNextStan());
    }

    @Test
    void shouldResetToOneAfter999999() {
        for (int i = 1; i <= 999_999; i++) {
            generator.getNextStan();
        }

        assertEquals("000001", generator.getNextStan());
    }
}
