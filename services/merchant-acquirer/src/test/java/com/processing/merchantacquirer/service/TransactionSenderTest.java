package com.processing.merchantacquirer.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.merchantacquirer.metrics.TransactionMetrics;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionSenderTest {
    private final GatewayClient gatewayClient = mock(GatewayClient.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final TransactionSender transactionSender = new TransactionSender(gatewayClient, new TransactionMetrics(registry));

    private AuthorizationRequest request(String stan, String mcc){
        return AuthorizationRequest.builder()
                .mti("0100")
                .stan(stan)
                .pan("40000012345678901234")
                .processingCode("000000")
                .amount(new BigDecimal("1000"))
                .currencyCode("654")
                .transmissionDateTime(Instant.now())
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000004")
                .mcc(mcc)
                .acquirerId("ACQ001")
                .build();
    }

    private double counter(String status, String mcc) {
        return registry.get("simulator.transactions").tag("status", status).tag("mcc", mcc).counter().count();
    }

    @Test
    void countsApprovedAndDeclinedByGatewayStatus() {
        when(gatewayClient.processAuthorize(any())).thenAnswer(inv -> {
            AuthorizationRequest r = inv.getArgument(0);
            return "5411".equals(r.mcc())
                    ? new AuthorizationResponse("0110", r.stan(), "rrn", "auth", "00", "APPROVED", null, 5)
                    : new AuthorizationResponse("0110", r.stan(), null, null, "05", "DECLINED", "reason", 5);
        });

        SimulatorStats stats = transactionSender.sendAll(List.of(
                request("000001", "5411"), request("000002", "5411"), request("000003", "5411"),
                request("000004", "5412"), request("000005", "5412"), request("000006", "5412")
        ), 1000);

        assertEquals(3, stats.approved());
        assertEquals(3, stats.declined());
        assertEquals(6, stats.responses().size());
        assertEquals(3.0, counter("APPROVED", "5411"));
        assertEquals(3.0, counter("DECLINED", "5412"));
    }

    @Test
    void gatewayReturnError() {
        when(gatewayClient.processAuthorize(any())).thenThrow(new ExternalServiceException("Gateway", "boom", "0"));

        SimulatorStats stats = transactionSender.sendAll(List.of(request("000001", "5411"), request("000002", "5411")), 1000);

        assertEquals(0, stats.approved());
        assertEquals(2, stats.declined());
        assertEquals("DECLINED", stats.responses().getFirst().status());
        assertEquals("96", stats.responses().getFirst().responseCode());
        assertEquals(2.0, counter("ERROR", "5411"));
    }

    @Test
    void emptyStats() {
        SimulatorStats stats = transactionSender.sendAll(List.of(), 1000);

        assertEquals(0, stats.responses().size());
        assertEquals(0, stats.approved());
        assertEquals(0, stats.declined());
    }
}
