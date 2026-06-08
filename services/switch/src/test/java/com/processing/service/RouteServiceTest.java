package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.support.CapturingAuthorizationClient;
import com.processing.support.TrackingLoggerClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteServiceTest {

    private final RoutingService routingService = new RoutingService(SwitchTestData.defaultProperties());

    @Test
    void route_unknownBin_declinesWithoutCallingDownstream() {
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService,
                new AuthorizationClient(SwitchTestData.defaultProperties(), null),
                logger);

        AuthorizationRequest request = new AuthorizationRequest(
                "0100", "000002", "9999991234560001", "000000", 150_000L, "643",
                SwitchTestData.sampleRequest().transmissionDateTime(),
                "TERM001", null, "MERCH12345678901", "5411", "ACQ001", null);

        AuthorizationResponse response = routeService.route(request);

        assertThat(response.responseCode()).isEqualTo("14");
        assertThat(response.status()).isEqualTo("DECLINED");
        assertThat(logger.wasCalled()).isFalse();
    }

    @Test
    void route_knownBin_authorizesAndLogsTransaction() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(routingService, authorizationClient, logger);

        AuthorizationRequest request = SwitchTestData.sampleRequest();
        AuthorizationResponse response = routeService.route(request);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(authorizationClient.lastRequest().issuerId()).isEqualTo("ISS001");
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().issuerId()).isEqualTo("ISS001");
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(logger.lastTransaction().mti()).isEqualTo("0100");
        assertThat(logger.lastTransaction().pan()).isEqualTo(request.pan());
    }

    @Test
    void route_loggerFailure_stillReturnsAuthorizationResponse() {
        TrackingLoggerClient logger = new TrackingLoggerClient(false);
        RouteService routeService = new RouteService(
                routingService,
                new AuthorizationClient(SwitchTestData.defaultProperties(), null),
                logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(logger.wasCalled()).isTrue();
    }
}
