package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingServiceTest {

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new RoutingService(SwitchTestData.defaultProperties());
    }

    @Test
    void getIssuerIdByPan_resolvesAllConfiguredBins() {
        assertThat(routingService.getIssuerIdByPan("4000001234560001")).isEqualTo("ISS001");
        assertThat(routingService.getIssuerIdByPan("4000011234560001")).isEqualTo("ISS002");
        assertThat(routingService.getIssuerIdByPan("4000041234560001")).isEqualTo("ISS005");
    }

    @Test
    void getIssuerIdByPan_throwsExceptionForUnknownBin() {
        assertThrows(UnknownBinException.class, () ->
                routingService.getIssuerIdByPan("9999991234560001"));
    }

    @Test
    void getIssuerIdByPan_throwsExceptionForShortOrMissingPan() {
        assertThrows(UnknownBinException.class, () ->
                routingService.getIssuerIdByPan("40000"));
        assertThrows(UnknownBinException.class, () ->
                routingService.getIssuerIdByPan(null));
    }

    @Test
    void constructor_copiesBinTableFromProperties() {
        SwitchProperties custom = new SwitchProperties(
                "1.0.0",
                java.util.Map.of("499999", "ISS999"),
                "http://auth",
                "http://logger",
                "http://merchant",
                SwitchTestData.defaultHttp(),
                SwitchTestData.defaultRetry()
        );
        RoutingService customRouting = new RoutingService(custom);

        assertThat(customRouting.getIssuerIdByPan("4999991234560001")).isEqualTo("ISS999");
        assertThrows(UnknownBinException.class, () ->
                customRouting.getIssuerIdByPan("4000001234560001"));
    }
}
