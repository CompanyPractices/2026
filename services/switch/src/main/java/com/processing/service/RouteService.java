package com.processing.service;


import com.processing.enums.TransactionStatus;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import com.processing.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;


@Service
public class RouteService {


    private static final Logger log = LoggerFactory.getLogger(RouteService.class);


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


        String issuerId = routingService.getIssuerIdByPan(pan);
        if (issuerId == null) {
            log.warn("TX {} | BIN={} → unknown BIN | DECLINED", request.stan(), bin);
            return AuthorizationResponse.unknownBin(request.stan());
        }


        AuthorizationRequest routedRequest = request.withIssuerId(issuerId);
        AuthorizationResponse response = authorizationClient.authorize(routedRequest);


        Transaction transaction = buildTransaction(routedRequest, response, issuerId);
        boolean logged = loggerClient.log(transaction);
        if (!logged && TransactionStatus.APPROVED.name().equals(response.status())) {
            // rollback + responseCode 96
            log.error("Logger unavailable for TX {} — APPROVED without audit trail", request.stan());
        }


        long elapsed = System.currentTimeMillis() - startMs;
        log.info("TX {} | BIN={} → {} | Status={} | {}ms",
                request.stan(), bin, issuerId, response.status(), elapsed);


        return response;
    }


    private Transaction buildTransaction(
            AuthorizationRequest request,
            AuthorizationResponse response,
            String issuerId) {
        return new Transaction(
                UUID.randomUUID(),
                request.mti(),
                request.stan(),
                response.rrn(),
                request.pan(),
                request.processingCode(),
                request.amount(),
                request.currencyCode(),
                request.terminalId(),
                request.merchantId(),
                request.mcc(),
                request.acquirerId(),
                issuerId,
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


    private static Instant toInstant(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return Instant.now();
        }
        return dateTime.atZone(ZoneOffset.UTC).toInstant();
    }
}
