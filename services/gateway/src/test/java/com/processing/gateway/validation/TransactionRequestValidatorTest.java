package com.processing.gateway.validation;

import com.processing.common.dto.authorization.AuthorizationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRequestValidatorTest {

    private final TransactionRequestValidator validator = new TransactionRequestValidator();

    @Test
    void acceptsValidAuthorizationRequest() {
        assertThatCode(() -> validator.validate(validRequest()))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsDecimalAmount() {
        AuthorizationRequest request = new AuthorizationRequest(
                "0100",
                "000001",
                "4000001234560001",
                "000000",
                new BigDecimal("150000.75"),
                "643",
                "2026-06-01T10:30:00Z",
                "TERM001",
                "POS",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPanWithWrongLength() {
        AuthorizationRequest request = new AuthorizationRequest(
                "0100",
                "000001",
                "400000123456000",
                "000000",
                BigDecimal.valueOf(150000),
                "643",
                "2026-06-01T10:30:00Z",
                "TERM001",
                "POS",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'pan' must be exactly 16 digits");
    }

    @Test
    void rejectsNonPositiveAmount() {
        AuthorizationRequest request = new AuthorizationRequest(
                "0100",
                "000001",
                "4000001234560001",
                "000000",
                BigDecimal.ZERO,
                "643",
                "2026-06-01T10:30:00Z",
                "TERM001",
                "POS",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'amount' must be > 0");
    }

    @Test
    void rejectsUnsupportedMti() {
        AuthorizationRequest request = new AuthorizationRequest(
                "0200",
                "000001",
                "4000001234560001",
                "000000",
                BigDecimal.valueOf(150000),
                "643",
                "2026-06-01T10:30:00Z",
                "TERM001",
                "POS",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("Field 'mti' must be '0100'");
    }

    private AuthorizationRequest validRequest() {
        return new AuthorizationRequest(
                "0100",
                "000001",
                "4000001234560001",
                "000000",
                BigDecimal.valueOf(150000),
                "643",
                "2026-06-01T10:30:00Z",
                "TERM001",
                "POS",
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );
    }

    @Test
    void acceptsRequestWithoutTerminalType() {
        AuthorizationRequest request = new AuthorizationRequest(
                "0100",
                "000001",
                "4000001234560000",
                "000000",
                BigDecimal.valueOf(150000),
                "643",
                "2026-06-01T10:30:00Z",
                "TERM001",
                null,
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }
}
