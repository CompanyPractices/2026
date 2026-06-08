package com.processing.service;


import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;


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
    void getIssuerIdByPan_returnsNullForUnknownBin() {
        assertThat(routingService.getIssuerIdByPan("9999991234560001")).isNull();
    }


    @Test
    void getIssuerIdByPan_returnsNullForShortOrMissingPan() {
        assertThat(routingService.getIssuerIdByPan("40000")).isNull();
        assertThat(routingService.getIssuerIdByPan(null)).isNull();
    }


    @Test
    void constructor_copiesBinTableFromProperties() {
        SwitchProperties custom = new SwitchProperties(
                "1.0.0",
                java.util.Map.of("499999", "ISS999"),
                "http://auth",
                "http://logger",
                new SwitchProperties.RetryProperties(3),
                2000
        );
        RoutingService customRouting = new RoutingService(custom);


        assertThat(customRouting.getIssuerIdByPan("4999991234560001")).isEqualTo("ISS999");
        assertThat(customRouting.getIssuerIdByPan("4000001234560001")).isNull();
    }
}
