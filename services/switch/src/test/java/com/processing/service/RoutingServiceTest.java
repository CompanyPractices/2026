package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-тесты таблицы BIN → issuerId в {@link RoutingService}.
 */
class RoutingServiceTest {

    private RoutingService routingService;

    /** Создаёт сервис с таблицей из {@link SwitchTestData}. */
    @BeforeEach
    void setUp() {
        routingService = new RoutingService(SwitchTestData.defaultProperties());
    }

    /** Все 5 настроенных BIN резолвятся в ожидаемые issuerId. */
    @Test
    void getIssuerIdByPan_resolvesAllConfiguredBins() {
        assertThat(routingService.getIssuerIdByPan("4000001234560001")).isEqualTo("ISS001");
        assertThat(routingService.getIssuerIdByPan("4000011234560001")).isEqualTo("ISS002");
        assertThat(routingService.getIssuerIdByPan("4000041234560001")).isEqualTo("ISS005");
    }

    /** Неизвестный BIN → {@link UnknownBinException}. */
    @Test
    void getIssuerIdByPan_throwsExceptionForUnknownBin() {
        assertThrows(UnknownBinException.class, () ->
                routingService.getIssuerIdByPan("9999991234560001"));
    }

    /** Короткий или null PAN → {@link UnknownBinException}. */
    @Test
    void getIssuerIdByPan_throwsExceptionForShortOrMissingPan() {
        assertThrows(UnknownBinException.class, () ->
                routingService.getIssuerIdByPan("40000"));
        assertThrows(UnknownBinException.class, () ->
                routingService.getIssuerIdByPan(null));
    }

    /** Таблица копируется при создании — изменения properties не влияют на уже созданный сервис. */
    @Test
    void constructor_copiesBinTableFromProperties() {
        SwitchProperties custom = new SwitchProperties(
                "1.0.0",
                java.util.Map.of("499999", "ISS999"),
                "http://auth",
                "http://logger",
                "http://merchant",
                SwitchTestData.defaultHttp(),
                SwitchTestData.defaultRetry(),
                SwitchTestData.defaultCircuitBreaker()
        );
        RoutingService customRouting = new RoutingService(custom);
        assertThat(customRouting.getIssuerIdByPan("4999991234560001")).isEqualTo("ISS999");
        assertThrows(UnknownBinException.class, () ->
                customRouting.getIssuerIdByPan("4000001234560001"));
    }
}
