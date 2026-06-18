package com.processing.merchantacquirer.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaskerPanTest {
    @Test
    void nullStars() {
        assertEquals("****", MaskerPan.mask(null));
    }

    @Test
    void shorterThanTenStarts() {
        assertEquals("****", MaskerPan.mask("123456789"));
    }

    @Test
    void validPan() {
        assertEquals("400000******4103", MaskerPan.mask("4000008516004103"));
    }
}
