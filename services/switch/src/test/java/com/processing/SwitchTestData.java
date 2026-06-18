package com.processing;


import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.config.SwitchProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;


/**
 * Общие тестовые данные и фабрики {@link SwitchProperties} для unit-тестов Switch.
 */
public final class SwitchTestData {


    /** Тип терминала по умолчанию в тестовых запросах. */
    public static final String TERMINAL_TYPE = "POS";

    /** Таблица BIN → issuerId, совпадающая с {@code application.yml}. */
    public static final Map<String, String> BIN_ROUTING = Map.of(
            "400000", "ISS001",
            "400001", "ISS002",
            "400002", "ISS003",
            "400003", "ISS004",
            "400004", "ISS005"
    );


    private SwitchTestData() {
    }


    /**
     * @return HTTP-таймауты по умолчанию для тестов
     */
    public static SwitchProperties.HttpProperties defaultHttp() {
        return new SwitchProperties.HttpProperties(3000, 5000, 2000);
    }


    /**
     * @return retry с нулевым backoff для быстрых unit-тестов
     */
    public static SwitchProperties.RetryProperties defaultRetry() {
        return new SwitchProperties.RetryProperties(3, List.of(0L, 0L, 0L));
    }


    /**
     * @return circuit breaker с production-подобными параметрами
     */
    public static SwitchProperties.CircuitBreakerSection defaultCircuitBreaker() {
        return new SwitchProperties.CircuitBreakerSection(
                new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 30_000L));
    }


    /**
     * @return circuit breaker, открывающийся после одного неуспешного вызова (для тестов)
     */
    public static SwitchProperties.CircuitBreakerSection aggressiveCircuitBreaker() {
        return new SwitchProperties.CircuitBreakerSection(
                new SwitchProperties.CircuitBreakerProperties(50, 1, 1, 30_000L));
    }


    /**
     * @return полная конфигурация Switch для локальных тестов
     */
    public static SwitchProperties defaultProperties() {
        return new SwitchProperties(
                "1.0.0",
                BIN_ROUTING,
                "http://localhost:8083",
                "http://localhost:8088",
                "http://localhost:8086",
                defaultHttp(),
                defaultRetry(),
                defaultCircuitBreaker()
        );
    }


    /**
     * @return типовой {@link AuthorizationRequest} с PAN BIN 400000
     */
    public static AuthorizationRequest sampleRequest() {
        return new AuthorizationRequest(
                "0100",
                "000001",
                "4000001234560001",
                "000000",
                new BigDecimal("150000"),
                "643",
                Instant.parse("2026-06-01T10:30:00Z"),
                "TERM001",
                TERMINAL_TYPE,
                "MERCH12345678901",
                "5411",
                "ACQ001",
                null
        );
    }
}
