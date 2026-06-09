package com.processing.service;


import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.transaction.Transaction;
import com.processing.common.dto.transaction.TransactionStatus;
import com.processing.exception.AuthorizationException;
import com.processing.exception.LoggerException;
import com.processing.exception.UnknownBinException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


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
    private final LoggerClient loggerClient;


    public RouteService(
            RoutingService routingService,
            AuthorizationClient authorizationClient,
            LoggerClient loggerClient) {
        this.routingService = routingService;
        this.authorizationClient = authorizationClient;
        this.loggerClient = loggerClient;
    }


    public AuthorizationResponse route(AuthorizationRequest request) {
        long startMs = System.currentTimeMillis();
        String pan = request.pan();
        String bin = pan != null && pan.length() >= 6 ? pan.substring(0, 6) : "??????";


        String issuerId;
        try {
            issuerId = routingService.getIssuerIdByPan(pan);
        } catch (UnknownBinException e) {
            LOG.warn("TX {} | BIN={} → unknown BIN | DECLINED", request.stan(), bin);
            return AuthorizationResponse.unknownBin(request.stan());
        }


        AuthorizationRequest routedRequest = request.withIssuerId(issuerId);


        AuthorizationResponse response;
        try {
            response = authorizationClient.authorize(routedRequest);
        } catch (AuthorizationException e) {
            LOG.error("{}", e.getMessage());
            return AuthorizationResponse.authUnavailable(request.stan());
        }


        Transaction transaction = buildTransaction(routedRequest, response);


        boolean logged;
        try {
            logged = loggerClient.log(transaction);
        } catch (LoggerException e) {
            LOG.error("{}", e.getMessage());
            logged = false;
        }


        if (!logged && TransactionStatus.APPROVED.name().equals(response.status())) {
            LOG.error("Logger unavailable for TX {} — rolling back reservation", request.stan());
            authorizationClient.reverse(routedRequest, response.rrn());
            return AuthorizationResponse.systemError(request.stan());
        }


        long elapsed = System.currentTimeMillis() - startMs;
        LOG.info("TX {} | BIN={} → {} | Status={} | {}ms",
                request.stan(), bin, issuerId, response.status(), elapsed);


        return response;
    }


    private Transaction buildTransaction(
            AuthorizationRequest request,
            AuthorizationResponse response) {
        return new TransactionRequest(
                UUID.randomUUID(),
                request.mti(),
                request.stan(),
                response.rrn(),
                request.pan(),
                request.processingCode(),
                request.amount() != null ? request.amount().longValue() : null,
                request.currencyCode(),
                request.terminalId(),
                request.merchantId(),
                request.mcc(),
                request.acquirerId(),
                request.issuerId(),
                null,
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
