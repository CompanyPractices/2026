package com.processing.transactionlogger.validation;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TransactionRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequestPassesValidation() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidCoreFieldFormats(TransactionRequest request, String field) {
        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                arguments(request("00001", "4000001234560001", "000000", "643", "5411",
                        new BigDecimal("2250")), "stan"),
                arguments(request("000001", "400000123456000X", "000000", "643", "5411",
                        new BigDecimal("2250")), "pan"),
                arguments(request("000001", "4000001234560001", "00000X", "643", "5411",
                        new BigDecimal("2250")), "processingCode"),
                arguments(request("000001", "4000001234560001", "000000", "64X", "5411",
                        new BigDecimal("2250")), "currencyCode"),
                arguments(request("000001", "4000001234560001", "000000", "643", "54X1",
                        new BigDecimal("2250")), "mcc"),
                arguments(request("000001", "4000001234560001", "000000", "643", "5411",
                        new BigDecimal("-1")), "acquiringFee")
        );
    }

    private static TransactionRequest validRequest() {
        return request("000001", "4000001234560001", "000000", "643", "5411",
                new BigDecimal("2250"));
    }

    private static TransactionRequest request(String stan,
                                              String pan,
                                              String processingCode,
                                              String currencyCode,
                                              String mcc,
                                              BigDecimal acquiringFee) {
        return new TransactionRequest(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "0100",
                stan,
                "012345678901",
                pan,
                processingCode,
                new BigDecimal("150000"),
                currencyCode,
                "TERM001",
                "POS",
                "MERCH1234567890",
                mcc,
                "ACQ001",
                "ISS001",
                acquiringFee,
                TransactionStatus.APPROVED,
                null,
                "ABC123",
                42,
                Instant.parse("2026-06-01T10:30:00Z"),
                Instant.parse("2026-06-01T10:30:01Z")
        );
    }
}
