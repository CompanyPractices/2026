package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.domain.repository.TerminalRepositoryPort;
import com.processing.merchantacquirer.domain.service.FeeCalculator;
import com.processing.merchantacquirer.domain.service.StanGenerator;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.model.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import com.processing.merchantacquirer.service.dto.RequestFeeData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionBuilderTest {
    private final TerminalRepositoryPort terminalRepository = mock(TerminalRepositoryPort.class);
    private final TransactionBuilder transactionBuilder = new TransactionBuilder(
            new AuthorizationRequestFactory(new StanGenerator()),
            new TerminalProvider(terminalRepository),
            new FeeCalculator()
    );
    private final Terminal terminal = new Terminal("TERM0001", "POS", null);

    private Merchant merchant(BigDecimal averageCheck) {
        when(terminalRepository.findByMerchantId("MERCH0000000004")).thenReturn(List.of(terminal));
        return new Merchant("MERCH0000000004", "STORE", "5454", "grocery", "ACQ001", new BigDecimal("0.015"), averageCheck);
    }

    private CardDataResponse card() {
        return new CardDataResponse("id", "40000012345678901234", "400000", "VANYA", "1234", "ACTIVE", "543", "0", "0", "0", "ISS001", Instant.now().toString());
    }

    @Test
    void generateAmountFromLowerBound() {
        Merchant merchant = merchant(new BigDecimal("100"));
        Scenario scenario = new Scenario(List.of("5454"), new BigDecimal("10000"), new BigDecimal("999999999"), "08:00", "22:00", 95);
        List<RequestFeeData> built = transactionBuilder.build(2, List.of(card()), List.of(merchant), scenario);

        assertEquals(2, built.size());
        assertEquals(new BigDecimal("10000"), built.get(0).authorizationRequest().amount());
        assertEquals(new BigDecimal("10000"), built.get(1).authorizationRequest().amount());
        assertEquals(new BigDecimal("150"), built.get(0).fee().getAcquirerFee());
    }

    @Test
    void generateAmountFromUpperBound() {
        Merchant merchant = merchant(new BigDecimal("100000000"));
        Scenario scenario = new Scenario(List.of("5454"), new BigDecimal("10000"), new BigDecimal("300000"), "08:00", "22:00", 95);
        List<RequestFeeData> built = transactionBuilder.build(2, List.of(card()), List.of(merchant), scenario);

        assertEquals(2, built.size());
        assertEquals(new BigDecimal("300000"), built.get(0).authorizationRequest().amount());
        assertEquals(new BigDecimal("300000"), built.get(1).authorizationRequest().amount());
    }

    @Test
    void emptyCards() {
        Merchant merchant = merchant(new BigDecimal("100000000"));
        Scenario scenario = new Scenario(List.of("5454"), new BigDecimal("10000"), new BigDecimal("300000"), "08:00", "22:00", 95);

        assertThrows(IllegalStateException.class, () -> transactionBuilder.build(1, List.of(), List.of(merchant), scenario));
    }
}
