package com.processing.terminalsimulator.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TerminalRunResponse;
import com.processing.common.dto.terminalsimulator.TerminalScenario;
import com.processing.common.dto.terminalsimulator.TerminalType;
import com.processing.terminalsimulator.TransactionStatus;
import com.processing.terminalsimulator.factory.TransactionFactory;
import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.model.PartofDay;
import com.processing.common.dto.terminalsimulator.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
    @Mock
    private TerminalCircuitBreaker terminalCircuitBreaker;

    private TerminalSimulatorService service;

    @BeforeEach
    void setUp() {
        int cardsAmount = 5000;
        int tps = 100;
        ScenarioTaskGenerator taskGenerator = new ScenarioTaskGenerator();
        service = new TerminalSimulatorService(gatewayClient, transactionFactory, taskGenerator, terminalCircuitBreaker,
                tps, cardsAmount);
        CardModel activeCard = new CardModel(UUID.randomUUID(), "4000001234560001", "400000", "IVAN IVANOV",
                YearMonth.of(2030, 1), CardModelStatus.ACTIVE, "643", new BigDecimal(500_002L),
                new BigDecimal(100_000L), new BigDecimal(20_000_000L), "ISS001",
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC));
        CardModel blockedCard = new CardModel(UUID.randomUUID(), "4000001234560003", "400000", "PETR PETROV",
                YearMonth.of(2029, 1), CardModelStatus.BLOCKED, "643", new BigDecimal(700_000L),
                new BigDecimal(200_000L), new BigDecimal(40_000L), "ISS001",
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC));

        when(terminalCircuitBreaker.isCallAllowed()).thenReturn(true);
        when(gatewayClient.getCardsFromCardManager(eq(CardModelStatus.ACTIVE),
                anyInt())).thenReturn(List.of(activeCard));
        when(gatewayClient.getCardsFromCardManager(eq(CardModelStatus.BLOCKED),
                anyInt())).thenReturn(List.of(blockedCard));
        when(transactionFactory.getRequiredStatus(any(TransactionType.class))).thenAnswer(invocation ->
        {
            TransactionType type = invocation.getArgument(0);
            return type == TransactionType.BLOCKED ? CardModelStatus.BLOCKED : CardModelStatus.ACTIVE;
        });

        when(transactionFactory.create(any(), any(), any(), any())).thenReturn(new AuthorizationRequest("0100",
                "000001", "4000001234560001", "000000", new BigDecimal(1000L), "643",
                (LocalDateTime.of(2026, 6, 5, 18, 12, 49)).toInstant(ZoneOffset.UTC)
                , "TERM001", String.valueOf(TerminalType.POS),
                "MERCH12345678901", "5411", "ACQ001", ""));
        when(gatewayClient.sendToGateway(any(AuthorizationRequest.class)))
                .thenReturn(new AuthorizationResponse("", "", "", "",
                        "", TransactionStatus.APPROVED.name(), "", 0));
    }

    @Test
    void run_normalScenario_TerminalSimulatorIsAlive() {
        TerminalRunResponse response = service.run(5, TerminalScenario.normal);

        assertEquals(5, response.totalSubmitted());
        assertEquals(5, response.approved());
        assertEquals(5, response.transactions().size());
        verify(gatewayClient, times(5)).sendToGateway(any(AuthorizationRequest.class));
    }

    @Test
    void run_normalScenario_callsFactoryWithCorrectType() {
        int totalCount = 10;
        service.run(totalCount, TerminalScenario.normal);

        verify(transactionFactory, times(totalCount)).create(eq(TransactionType.NORMAL), eq(PartofDay.DAY),
                any(CardModel.class), any(String.class));
    }

    @Test
    void run_nightTimeScenario_callsFactoryWithNightParam() {
        int totalCount = 10;
        service.run(totalCount, TerminalScenario.night_time);

        verify(transactionFactory, times(5)).create(eq(TransactionType.NORMAL),
                eq(PartofDay.NIGHT), any(CardModel.class), any(String.class));
        verify(transactionFactory, times(5)).create(eq(TransactionType.HIGH_VALUE),
                eq(PartofDay.NIGHT), any(CardModel.class), any(String.class));
    }

    @Test
    void run_highValueScenario_callsFactoryWithCorrectType() {
        int totalCount = 10;
        service.run(totalCount, TerminalScenario.high_value);

        verify(transactionFactory, times(totalCount)).create(eq(TransactionType.HIGH_VALUE), eq(PartofDay.DAY),
                any(CardModel.class), any(String.class));
    }

    @Test
    void run_mixedScenario_verifiesProportions() {
        int totalCount = 20;
        service.run(totalCount, TerminalScenario.mixed);

        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        verify(transactionFactory, times(totalCount)).create(typeCaptor.capture(), eq(PartofDay.DAY),
                any(CardModel.class), any(String.class));

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
        service.run(totalCount, TerminalScenario.declines_test);

        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        verify(transactionFactory, times(totalCount)).create(typeCaptor.capture(), eq(PartofDay.DAY),
                any(CardModel.class), any(String.class));

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
        when(gatewayClient.getCardsFromCardManager(eq(CardModelStatus.ACTIVE), anyInt())).thenReturn(null);
        assertThatThrownBy(() -> service.run(5, TerminalScenario.normal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ACTIVE cards available");
    }

    @Test
    void run_whenNoBlockedCardsAndScenarioRequiresThem_throwsException() {
        when(gatewayClient.getCardsFromCardManager(eq(CardModelStatus.BLOCKED), anyInt())).thenReturn(null);
        assertThatThrownBy(() -> service.run(5, TerminalScenario.mixed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No BLOCKED cards available");
    }
}
