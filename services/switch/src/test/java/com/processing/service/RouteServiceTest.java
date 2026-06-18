package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.support.CapturingAuthorizationClient;
import com.processing.support.FailingAuthorizationClient;
import com.processing.support.TrackingLoggerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты оркестрации {@link RouteService}: маршрутизация, логирование, rollback.
 */
class RouteServiceTest {

    private static final String TEST_RRN = "012345678901";
    private static final String TEST_PAN = "4000001234560001";
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(150000);

    private final RoutingService routingService = new RoutingService(SwitchTestData.defaultProperties());

    /** Неизвестный BIN → DECLINED/14, Logger вызывается, Authorization — нет. */
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
        assertThat(authorizationClient.rollbackCalled()).isFalse();
        assertThat(logger.wasCalled()).isTrue();
        assertThat(logger.lastTransaction().status()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(logger.lastTransaction().issuerId()).isNull();
    }

    /** Authorization недоступен → DECLINED/05, транзакция всё равно логируется. */
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

    /** Успешный цикл: BIN → ISS001, APPROVED, запись в Logger. */
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
        assertThat(authorizationClient.rollbackCalled()).isFalse();
    }

    /** Smoke-test идентификаторы нормализуются до отправки в Authorization. */
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
                Instant.parse("2026-06-01T10:30:00Z"),
                "TERM001", SwitchTestData.TERMINAL_TYPE, "MERCH00000000001", "5411", "ACQ001", null);

        AuthorizationResponse response = routeService.route(request);

        assertThat(response.status()).isIn(
                AuthorizationResponse.STATUS_APPROVED, AuthorizationResponse.STATUS_DECLINED);
        assertThat(authorizationClient.lastRequest().terminalId()).isEqualTo("TERM0010");
        assertThat(authorizationClient.lastRequest().merchantId()).isEqualTo("MERCH0000000001");
        assertThat(logger.lastTransaction().terminalId()).isEqualTo("TERM0010");
        assertThat(logger.lastTransaction().merchantId()).isEqualTo("MERCH0000000001");
    }

    /** Комиссия эквайринга из Merchant Acquirer попадает в TransactionRequest. */
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

    /**
     * Logger недоступен после APPROVED → rollback и DECLINED/96 независимо от кода rollback.
     *
     * @param rollbackResponse симулированный ответ Authorization на rollback
     */
    @ParameterizedTest(name = "rollback response code {0}")
    @MethodSource("rollbackResponses")
    void route_loggerFailureAfterApproved_triggersRollbackAndReturns96(RollbackResponse rollbackResponse) {
        CapturingAuthorizationClient authorizationClient = new CapturingAuthorizationClient()
                .withRollbackResponse(rollbackResponse);
        TrackingLoggerClient logger = new TrackingLoggerClient(false);
        RouteService routeService = new RouteService(
                routingService, authorizationClient, (transmissionDateTime, stan, pan, terminalId, amount) -> null, logger);

        AuthorizationResponse response = routeService.route(SwitchTestData.sampleRequest());

        assertThat(response.status()).isEqualTo(AuthorizationResponse.STATUS_DECLINED);
        assertThat(response.responseCode()).isEqualTo(AuthorizationResponse.CODE_SERVICE_UNAVAILABLE);
        assertThat(logger.wasCalled()).isTrue();
        assertThat(authorizationClient.rollbackCalled()).isTrue();
        assertThat(authorizationClient.lastRollbackRrn()).isEqualTo(TEST_RRN);
        assertThat(authorizationClient.lastRollbackRequest().rrn()).isEqualTo(TEST_RRN);
        assertThat(authorizationClient.lastRollbackRequest().pan()).isEqualTo("4000001234560001");
        assertThat(authorizationClient.lastRollbackRequest().amount())
                .isEqualByComparingTo(new BigDecimal("150000"));
    }

    /**
     * @return набор ответов rollback (успех и все decline-коды)
     */
    private static Stream<RollbackResponse> rollbackResponses() {
        Instant now = Instant.now();
        return Stream.of(
                RollbackResponse.approved(new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), now),
                RollbackResponse.declined(
                        new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "TRANSACTION_NOT_FOUND",
                        RollbackResponse.CODE_TRANSACTION_NOT_FOUND, now),
                RollbackResponse.declined(
                        new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "ALREADY_ROLLED_BACK",
                        RollbackResponse.CODE_DECLINED_GENERAL, now),
                RollbackResponse.declined(
                        new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "ROLLBACK_FAILED",
                        RollbackResponse.CODE_SERVICE_UNAVAILABLE, now),
                RollbackResponse.declined(
                        new RollbackRequest(TEST_RRN, TEST_PAN, TEST_AMOUNT), "SERVICE_UNAVAILABLE",
                        RollbackResponse.CODE_SERVICE_UNAVAILABLE, now)
        );
    }

    /** DECLINED от Authorization → логируется, rollback не вызывается. */
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
        assertThat(authorizationClient.rollbackCalled()).isFalse();
    }
}
