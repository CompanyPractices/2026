package com.processing.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class LuhnValidatorTest {
    private final LuhnValidator luhnValidator = new LuhnValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "4532015112830366",
            "4916338506082832",
            "5425233430109903",
            "2222420000001113",
            "6011111111111117"
    })
    void validPansShouldReturnTrue(String pan) {
        assertTrue(luhnValidator.isValid(pan));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4532115112833365",
            "4916331556082832",
            "5425233760189909",
            "2222456090101113",
            "6011117391111185"
    })
    void invalidPansShouldReturnFalse(String pan) {
        assertFalse(luhnValidator.isValid(pan));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "400000",
            "400001",
            "400002",
            "400003",
            "400004"
    })
    void generatePanShouldBeValid(String bin) {
        String pan = luhnValidator.generatePan(bin);
        assertTrue(luhnValidator.isValid(pan));
    }

    @Test
    void generatePanShouldHave16Digits() {
        String pan = luhnValidator.generatePan("400001");
        assertEquals(16, pan.length());
    }

    @Test
    void generatePanShouldStartWithBin() {
        String bin = "400002";
        String pan = luhnValidator.generatePan(bin);
        assertTrue(pan.startsWith(bin));
    }
}
