package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.support.CapturingAuthorizationClient;
import com.processing.support.FailingAuthorizationClient;
import com.processing.support.TrackingLoggerClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RouteServiceTest {

    private final RoutingService routingService = new RoutingService(SwitchTestData.defaultProperties());

    @Test
    void route_unknownBin_declinesAndLogsWithoutCallingAuthorization() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService,
                authorizationClient,
                (transmissionDateTime, stan, pan, terminalId, amount) -> null,
                logger);

        AuthorizationRequest request = new AuthorizationRequest(
                "0100", "000002", "9999991234560001", "000000", new BigDecimal("150000"), "643",
                SwitchTestData.sampleRequest().transmissionDateTime(),
                "TERM001", SwitchTestData.TERMINAL_TYPE, "MERCH12345678901", "5411", "ACQ001", null);

        AuthorizationResponse response = routeService.route(request);

        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_CARD_NOT_FOUND);
        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(authorizationClient.lastRequest()).isNull();
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(logger.lastTransaction().issuerId()).isNull();
    }

    @Test
    void route_authUnavailable_declinesAndLogsTransaction() {
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService,
                new FailingAuthorizationClient(),
                (transmissionDateTime, stan, pan, terminalId, amount) -> null,
                logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_DECLINED_GENERAL);
        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(logger.lastTransaction().issuerId()).isEqualTo("ISS001");
    }

    @Test
    void route_knownBin_authorizesAndLogsTransaction() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService, authorizationClient, (transmissionDateTime, stan, pan, terminalId, amount) -> null, logger);

        AuthorizationRequest request = SwitchTestData.sampleRequest();
        AuthorizationResponse response = routeService.route(request);

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_APPROVED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_APPROVED);
        assertThat(authorizationClient.lastRequest().issuerId()).isEqualTo("ISS001");
        assertThat(authorizationClient.lastRequest().terminalId()).isEqualTo("TERM0010");
        assertThat(authorizationClient.lastRequest().merchantId()).isEqualTo("MERCH1234567890");
        assertThat(authorizationClient.lastRequest().terminalType()).isEqualTo(SwitchTestData.TERMINAL_TYPE);
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().issuerId()).isEqualTo("ISS001");
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(logger.lastTransaction().mti()).isEqualTo("0100");
        assertThat(logger.lastTransaction().pan()).isEqualTo(request.pan());
        assertThat(logger.lastTransaction().amount()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(authorizationClient.lastRequest().amount()).isEqualByComparingTo(new BigDecimal("150000"));
    }

    @Test
    void route_smokeTestIds_areNormalizedBeforeAuthorization() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService,
                authorizationClient,
                (transmissionDateTime, stan, pan, terminalId, amount) -> null,
                logger);

        AuthorizationRequest request = new AuthorizationRequest(
                "0100", "000001", "4000001234560001", "000000", new BigDecimal("150000"), "643",
                "2026-06-01T10:30:00Z",
                "TERM001", SwitchTestData.TERMINAL_TYPE, "MERCH00000000001", "5411", "ACQ001", null);

        AuthorizationResponse response = routeService.route(request);

        assertThat(response.status()).isIn(
                AuthorizationResponse.STATUS_APPROVED, AuthorizationResponse.STATUS_DECLINED);
        assertThat(authorizationClient.lastRequest().terminalId()).isEqualTo("TERM0010");
        assertThat(authorizationClient.lastRequest().merchantId()).isEqualTo("MERCH0000000001");
        assertThat(logger.lastTransaction().terminalId()).isEqualTo("TERM0010");
        assertThat(logger.lastTransaction().merchantId()).isEqualTo("MERCH0000000001");
    }

    @Test
    void route_includesAcquiringFeeFromMerchantAcquirer() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService,
                authorizationClient,
                (transmissionDateTime, stan, pan, terminalId, amount) -> new BigDecimal("2250"),
                logger);

        routeService.route(SwitchTestData.sampleRequest());

        assertThat(logger.lastTransaction().acquiringFee()).isEqualByComparingTo(new BigDecimal("2250"));
    }

    @Test
    void route_loggerFailureAfterApproved_triggersRollbackAndReturns96() {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient();
        TrackingLoggerClient logger = new TrackingLoggerClient(false);
        RouteService routeService = new RouteService(
                routingService, authorizationClient, (transmissionDateTime, stan, pan, terminalId, amount) -> null, logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_SERVICE_UNAVAILABLE);
        assertThat(logger.wasCalled()).isTrue();
        assertThat(authorizationClient.reverseCalled()).isTrue();
        assertThat(authorizationClient.lastReverseRrn()).isEqualTo("012345678901");
    }

    @Test
    void route_declinedByAuth_stillLogsAndReturnsDecline() {
        AuthorizationResponse declined = new AuthorizationResponse(
                "0110", "000001", null, null,
                AuthorizationResponse.CODE_INSUFFICIENT_FUNDS,
                AuthorizationResponse.STATUS_DECLINED,
                "Insufficient funds", 10);
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient(declined);
        TrackingLoggerClient logger = new TrackingLoggerClient(true);
        RouteService routeService = new RouteService(
                routingService, authorizationClient, (transmissionDateTime, stan, pan, terminalId, amount) -> null, logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_INSUFFICIENT_FUNDS);
        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(authorizationClient.reverseCalled()).isFalse();
    }
}
