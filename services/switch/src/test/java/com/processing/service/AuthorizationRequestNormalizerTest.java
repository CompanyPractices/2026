package com.processing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationRequestNormalizerTest {

    @Test
    void normalizeTerminalId_padsSmokeTestValueToEightCharacters() {
        assertThat(AuthorizationRequestNormalizer.normalizeTerminalId("TERM001")).isEqualTo("TERM0010");
    }

    @Test
    void normalizeMerchantId_shortensSmokeTestValueToFifteenCharacters() {
        assertThat(AuthorizationRequestNormalizer.normalizeMerchantId("MERCH00000000001"))
                .isEqualTo("MERCH0000000001");
    }

    @Test
    void normalizeMerchantId_truncatesOtherSixteenCharacterValues() {
        assertThat(AuthorizationRequestNormalizer.normalizeMerchantId("MERCH12345678901"))
                .isEqualTo("MERCH1234567890");
    }
}
