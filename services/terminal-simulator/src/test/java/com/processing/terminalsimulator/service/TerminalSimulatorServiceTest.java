package com.processing.terminalsimulator.service;

import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.dto.AuthorizationRequest;
import com.processing.terminalsimulator.dto.AuthorizationResponse;
import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.dto.RunResponse;
import com.processing.terminalsimulator.factory.TransactionFactory;
import com.processing.terminalsimulator.model.Scenario;
import com.processing.terminalsimulator.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.processing.terminalsimulator.model.CardStatus.ACTIVE;
import static com.processing.terminalsimulator.model.CardStatus.BLOCKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminalSimulatorServiceTest {

    @Mock
    private GatewayClient gatewayClient;

    @Mock
    private TransactionFactory transactionFactory;

    @InjectMocks
    private TerminalSimulatorService service;

    private Card activeCard;
    private Card blockedCard;

    @BeforeEach
    void setUp() {
        activeCard = new Card("id1", "4000001234560001", "400000",
                "IVAN IVANOV", "1228", ACTIVE, "643", 500_002,
                100_000, 20_000_000, "ISS001", "2026-06-01T10:00:00Z");
        blockedCard = new Card("id2", "4000001234560003", "400000",
                "PETR PETROV", "1228", BLOCKED, "643", 700_000,
                200_000, 40_000, "ISS001", "2026-06-01T10:00:00Z");

        when(gatewayClient.getCardsFromCardManager(ACTIVE, 70)).thenReturn(List.of(activeCard));
        when(gatewayClient.getCardsFromCardManager(BLOCKED, 30)).thenReturn(List.of(blockedCard));
        when(transactionFactory.getRequiredStatus(any(TransactionType.class))).thenAnswer(invocation ->
        {
            TransactionType type = invocation.getArgument(0);
            return type == TransactionType.BLOCKED ? BLOCKED : ACTIVE;
        });

        when(transactionFactory.create(any(), any(), any())).thenReturn(new AuthorizationRequest("0100",
                "000001", "4000001234560001", "000000", 1000L, "643",
                "2026-06-01T10:00:00Z", "TERM001", "POS",
                "MERCH12345678901", "5411", "ACQ001", ""));
        when(gatewayClient.sendToGateway(any(AuthorizationRequest.class)))
                .thenReturn(new AuthorizationResponse("", "", "", "",
                        "", "APPROVED", "", 0));
    }

    @Test
    void run_normalScenario_TerminalSimulatorIsAlive() {
        RunResponse response = service.run(5, Scenario.normal);

        assertEquals(5, response.totalSubmitted());
        assertEquals(5, response.approved());
        assertThat(response.transactions()).hasSize(5);
        verify(gatewayClient, times(5)).sendToGateway(any(AuthorizationRequest.class));
    }

    @Test
    void run_normalScenario_callsFactoryWithCorrectType() {
        int totalCount = 10;
        service.run(totalCount, Scenario.normal);

        verify(transactionFactory, times(totalCount)).create(eq(TransactionType.NORMAL), eq("day"),
                any(Card.class));
    }

    @Test
    void run_nightTimeScenario_callsFactoryWithNightParam() {
        int totalCount = 10;
        service.run(totalCount, Scenario.night_time);

        verify(transactionFactory, times(5)).create(eq(TransactionType.NORMAL),
                eq("night"), any(Card.class));
        verify(transactionFactory, times(5)).create(eq(TransactionType.HIGH_VALUE),
                eq("night"), any(Card.class));
    }

    @Test
    void run_highValueScenario_callsFactoryWithCorrectType() {
        int totalCount = 10;
        service.run(totalCount, Scenario.high_value);

        verify(transactionFactory, times(totalCount)).create(eq(TransactionType.HIGH_VALUE), eq("day"),
                any(Card.class));
    }

    @Test
    void run_mixedScenario_verifiesProportions() {
        int totalCount = 20;
        service.run(totalCount, Scenario.mixed);

        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        verify(transactionFactory, times(totalCount)).create(typeCaptor.capture(), eq("day"), any(Card.class));

        List<TransactionType> calledTypes = typeCaptor.getAllValues();

        long normalCount = calledTypes.stream().filter(t -> t == TransactionType.NORMAL).count();
        long highValueCount = calledTypes.stream().filter(t -> t == TransactionType.HIGH_VALUE).count();
        long almostDailyCount = calledTypes.stream().filter(t -> t ==
                TransactionType.ALMOST_DAILY_LIMIT).count();
        long blockedCount = calledTypes.stream().filter(t -> t == TransactionType.BLOCKED).count();

        assertEquals(14, normalCount);
        assertEquals(3, highValueCount);
        assertEquals(2, almostDailyCount);
        assertEquals(1, blockedCount);
    }

    @Test
    void run_declinesScenario_verifiesProportions() {
        int totalCount = 10;
        service.run(totalCount, Scenario.declines_test);

        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        verify(transactionFactory, times(totalCount)).create(typeCaptor.capture(), eq("day"), any(Card.class));

        List<TransactionType> calledTypes = typeCaptor.getAllValues();

        assertEquals(2, calledTypes.stream().filter(t -> t ==
                TransactionType.INVALID_PAN).count());
        assertEquals(2, calledTypes.stream().filter(t -> t == TransactionType.BLOCKED).count());
        assertEquals(2, calledTypes.stream().filter(t -> t == TransactionType.NO_MONEY).count());
        assertEquals(2, calledTypes.stream().filter(t -> t ==
                TransactionType.MORE_THAN_DAILY_LIMIT).count());
        assertEquals(2, calledTypes.stream().filter(t -> t == TransactionType.NORMAL).count());
    }

    @Test
    void run_whenNoActiveCards_throwsException() {
        when(gatewayClient.getCardsFromCardManager(ACTIVE, 70)).thenReturn(null);
        assertThatThrownBy(() -> service.run(5, Scenario.normal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ACTIVE cards available");
    }

    @Test
    void run_whenNoBlockedCardsAndScenarioRequiresThem_throwsException() {
        when(gatewayClient.getCardsFromCardManager(BLOCKED, 30)).thenReturn(null);
        assertThatThrownBy(() -> service.run(5, Scenario.mixed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No BLOCKED cards available");
    }
}
