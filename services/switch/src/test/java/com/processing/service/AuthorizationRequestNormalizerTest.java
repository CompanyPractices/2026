package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit-тесты нормализации {@link AuthorizationRequest} в {@link AuthorizationRequestNormalizer}.
 */
class AuthorizationRequestNormalizerTest {

    /** TERM001 дополняется до 8 символов. */
    @Test
    void normalizeTerminalId_padsSmokeTestValueToEightCharacters() {
        assertThat(AuthorizationRequestNormalizer.normalizeTerminalId("TERM001")).isEqualTo("TERM0010");
    }

    /** MERCH00000000001 (16 символов) с лишним нулём сокращается до 15. */
    @Test
    void normalizeMerchantId_shortensSmokeTestValueToFifteenCharacters() {
        assertThat(AuthorizationRequestNormalizer.normalizeMerchantId("MERCH00000000001"))
                .isEqualTo("MERCH0000000001");
    }

    /** Прочие 16-символьные merchantId обрезаются с конца. */
    @Test
    void normalizeMerchantId_truncatesOtherSixteenCharacterValues() {
        assertThat(AuthorizationRequestNormalizer.normalizeMerchantId("MERCH12345678901"))
                .isEqualTo("MERCH1234567890");
    }

    /** Дробная сумма не теряет точность при normalize. */
    @Test
    void shouldPreserveFractionalAmountWithoutPrecisionLoss() {
        BigDecimal amountWithKopecks = new BigDecimal("150.50");

        AuthorizationRequest original = AuthorizationRequest.builder()
                .mti("0100")
                .stan("000001")
                .pan("4000001234560001")
                .processingCode("000000")
                .amount(amountWithKopecks)
                .currencyCode("643")
                .transmissionDateTime(Instant.parse("2026-06-01T10:30:00Z"))
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000001")
                .mcc("5411")
                .acquirerId("ACQ001")
                .issuerId("ISS001")
                .build();

        AuthorizationRequest normalized = AuthorizationRequestNormalizer.normalize(original);

        assertEquals(new BigDecimal("150.50"), normalized.amount());
    }
}
