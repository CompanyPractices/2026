package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.exception.AuthorizationException;
import com.processing.exception.LoggerException;
import com.processing.exception.UnknownBinException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    private final RoutingService routingService;
    private final AuthorizationClient authorizationClient;
    private final AcquiringFeeClient acquiringFeeClient;
    private final LoggerClient loggerClient;

    public RouteService(
            RoutingService routingService,
            AuthorizationClient authorizationClient,
            AcquiringFeeClient acquiringFeeClient,
            LoggerClient loggerClient) {
        this.routingService = routingService;
        this.authorizationClient = authorizationClient;
        this.acquiringFeeClient = acquiringFeeClient;
        this.loggerClient = loggerClient;
    }

    public AuthorizationResponse route(AuthorizationRequest request) {
        long startMs = System.currentTimeMillis();
        String pan = request.pan();
        String bin = pan != null && pan.length() >= 6 ? pan.substring(0, 6) : "??????";

        AuthorizationRequest normalizedRequest = AuthorizationRequestNormalizer.normalize(request);
        AuthorizationRequest routedRequest = normalizedRequest;
        AuthorizationResponse response;
        BigDecimal acquiringFee = null;
        String issuerId = null;

        try {
            issuerId = routingService.getIssuerIdByPan(pan);
            routedRequest = normalizedRequest.withIssuerId(issuerId);
            response = authorizationClient.authorize(routedRequest);
            acquiringFee = acquiringFeeClient.fetchAcquiringFee(
                    routedRequest.transmissionDateTime(),
                    routedRequest.stan(),
                    routedRequest.pan(),
                    routedRequest.terminalId(),
                    routedRequest.amount());
        } catch (UnknownBinException e) {
            LOG.warn("TX {} | BIN={} → unknown BIN | DECLINED", request.stan(), bin);
            response = AuthorizationResponse.unknownBin(request.stan());
        } catch (AuthorizationException e) {
            LOG.error("{}", e.getMessage());
            response = AuthorizationResponse.authUnavailable(request.stan());
        }

        TransactionRequest transaction = buildTransaction(routedRequest, response, acquiringFee);
        boolean logged = tryLog(transaction);

        if (!logged && AuthorizationResponse.STATUS_APPROVED.equals(response.status())) {
            LOG.error("Logger unavailable for TX {} — rolling back reservation", request.stan());
            authorizationClient.reverse(routedRequest, response.rrn());
            return AuthorizationResponse.systemError(request.stan());
        }

        long elapsed = System.currentTimeMillis() - startMs;
        LOG.info("TX {} | BIN={} → {} | Status={} | {}ms",
                request.stan(), bin, issuerId, response.status(), elapsed);

        return response;
    }

    private boolean tryLog(TransactionRequest transaction) {
        try {
            return loggerClient.log(transaction);
        } catch (LoggerException e) {
            LOG.error("{}", e.getMessage());
            return false;
        }
    }

    private TransactionRequest buildTransaction(
            AuthorizationRequest request,
            AuthorizationResponse response,
            BigDecimal acquiringFee) {
        return new TransactionRequest(
                UUID.randomUUID(),
                request.mti(),
                request.stan(),
                response.rrn(),
                request.pan(),
                request.processingCode(),
                request.amount(),
                request.currencyCode(),
                request.terminalId(),
                request.terminalType(),
                request.merchantId(),
                request.mcc(),
                request.acquirerId(),
                request.issuerId(),
                acquiringFee,
                toTransactionStatus(response.status()),
                response.declineReason(),
                response.authCode(),
                response.processingTimeMs() != null ? response.processingTimeMs() : 0,
                toInstant(request.transmissionDateTime()),
                Instant.now()
        );
    }

    private static TransactionStatus toTransactionStatus(String status) {
        return TransactionStatus.valueOf(status);
    }

    private static Instant toInstant(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(dateTime);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(dateTime).atZone(ZoneOffset.UTC).toInstant();
        }
    }
}
