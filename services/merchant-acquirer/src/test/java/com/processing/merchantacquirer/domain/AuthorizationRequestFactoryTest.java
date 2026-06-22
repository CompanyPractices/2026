package com.processing.merchantacquirer.domain;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizationRequestFactoryTest {
    private final AuthorizationRequestFactory authorizationRequestFactory = new AuthorizationRequestFactory(new StanGenerator());
    private final Merchant
            merchant = new Merchant("MERCH0000000009", "STORE", "5454", "grocery", "ACQ007", new BigDecimal("0.015"), new BigDecimal("120000"));
    private final Terminal terminal = new Terminal("TERM0001", "POS", merchant);

    @Test
    void mapsAllFields(){
        AuthorizationRequest request = authorizationRequestFactory.build(
                "40000012345678911234",
                "643",
                new BigDecimal("1000"),
                terminal,
                merchant,
                Instant.now()
        );

        assertEquals("0100", request.mti());
        assertEquals("000000", request.processingCode());
        assertEquals("000001", request.stan());
        assertEquals("40000012345678911234", request.pan());
        assertEquals("643", request.currencyCode());
        assertEquals(new BigDecimal("1000"), request.amount());
        assertEquals("TERM0001", request.terminalId());
        assertEquals("POS", request.terminalType());
        assertEquals("MERCH0000000009", request.merchantId());
        assertEquals("5454", request.mcc());
        assertEquals("ACQ007", request.acquirerId());
        assertNull(request.issuerId());
    }

    @Test
    void correctStanGenerated() {
        AuthorizationRequest request1 = authorizationRequestFactory.build(
                "40000012345678911234",
                "643",
                new BigDecimal("1000"),
                terminal,
                merchant,
                Instant.now()
        );
        AuthorizationRequest request2 = authorizationRequestFactory.build(
                "40000012345678911235",
                "643",
                new BigDecimal("1000"),
                terminal,
                merchant,
                Instant.now()
        );

        assertEquals("000001", request1.stan());
        assertEquals("000002", request2.stan());
    }
}
