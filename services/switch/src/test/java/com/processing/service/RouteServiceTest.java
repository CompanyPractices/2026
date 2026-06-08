package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.enums.TransactionStatus;
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
                new CapturingAuthorizationClient(),
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
        assertThat(logger.lastTransaction().stan()).isEqualTo(request.stan());
        assertThat(logger.lastTransaction().processingCode()).isEqualTo(request.processingCode());
        assertThat(logger.lastTransaction().amount()).isEqualTo(request.amount());
        assertThat(logger.lastTransaction().currencyCode()).isEqualTo(request.currencyCode());
        assertThat(logger.lastTransaction().terminalId()).isEqualTo(request.terminalId());
        assertThat(logger.lastTransaction().merchantId()).isEqualTo(request.merchantId());
        assertThat(logger.lastTransaction().mcc()).isEqualTo(request.mcc());
        assertThat(logger.lastTransaction().acquirerId()).isEqualTo(request.acquirerId());
        assertThat(logger.lastTransaction().rrn()).isEqualTo("012345678901");
        assertThat(logger.lastTransaction().authCode()).isEqualTo("TEST01");
        assertThat(logger.lastTransaction().processingTimeMs()).isEqualTo(42L);
        assertThat(logger.lastTransaction().id()).isNotNull();
        assertThat(logger.lastTransaction().createdAt()).isNotNull();
        assertThat(logger.lastTransaction().transmissionDateTime()).isNotNull();
    }

    @Test
    void route_declinedByAuth_stillLogsToLogger() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient()
                .withResponse(CapturingAuthorizationClient.declinedFixture("000001", "51"));
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(routingService, authorizationClient, logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.responseCode()).isEqualTo("51");
        assertThat(response.status()).isEqualTo("DECLINED");
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(authorizationClient.reverseCalled()).isFalse();
    }

    @Test
    void route_loggerFailureAfterApproved_triggersReversalAndReturns96() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(false);
        RouteService routeService = new RouteService(routingService, authorizationClient, logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.status()).isEqualTo("DECLINED");
        assertThat(response.responseCode()).isEqualTo("96");
        assertThat(logger.wasCalled()).isTrue();
        assertThat(authorizationClient.reverseCalled()).isTrue();
        assertThat(authorizationClient.lastReversalRequest().mti()).isEqualTo("0400");
    }

    @Test
    void route_loggerFailureAfterDeclined_doesNotReverse() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient()
                .withResponse(CapturingAuthorizationClient.declinedFixture("000001", "51"));
        TrackingLoggerClient logger = new TrackingLoggerClient(false);
        RouteService routeService = new RouteService(routingService, authorizationClient, logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.responseCode()).isEqualTo("51");
        assertThat(authorizationClient.reverseCalled()).isFalse();
    }
}
