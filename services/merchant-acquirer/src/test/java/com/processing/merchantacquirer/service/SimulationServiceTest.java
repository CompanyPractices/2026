package com.processing.merchantacquirer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.controller.dto.SimulatorRequest;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.ScenarioType;
import com.processing.merchantacquirer.service.dto.RequestFeeData;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock private CardProvider cardProvider;
    @Mock private MerchantProvider merchantProvider;
    @Mock private TransactionBuilder transactionBuilder;
    @Mock private TransactionSender transactionSender;
    @Mock private ScenarioProvider scenarioProvider;

    @InjectMocks private SimulationService simulationService;

    @Test
    void endToEndAcrossRealBeans() {
        SimulatorRequest request = new SimulatorRequest(3, ScenarioType.grocery, null);
        List<CardDataResponse> mockCards = List.of(mock(CardDataResponse.class), mock(CardDataResponse.class));
        Scenario mockScenario = mock(Scenario.class);
        List<Merchant> mockMerchants = List.of(mock(Merchant.class));
        List<RequestFeeData> mockBuilt = List.of(
                new RequestFeeData(mock(AuthorizationRequest.class), mock(AcquirerFee.class)),
                new RequestFeeData(mock(AuthorizationRequest.class), mock(AcquirerFee.class)),
                new RequestFeeData(mock(AuthorizationRequest.class), mock(AcquirerFee.class))
        );
        SimulatorStats mockStats = mock(SimulatorStats.class);
        when(mockStats.approved()).thenReturn(2);
        when(mockStats.declined()).thenReturn(1);

        List<AuthorizationResponse> mockTransactions = List.of(
                mock(AuthorizationResponse.class),
                mock(AuthorizationResponse.class),
                mock(AuthorizationResponse.class)
        );

        when(mockStats.responses()).thenReturn(mockTransactions);
        when(cardProvider.getCards(3)).thenReturn(mockCards);
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(null, mockScenario)).thenReturn(mockMerchants);
        when(transactionBuilder.build(anyInt(), any(), any(), any()))
                .thenReturn(mockBuilt);
        when(transactionSender.sendAll(any())).thenReturn(mockStats);

        SimulatorResponse response = simulationService.run(request);

        assertEquals(3, response.totalSubmitted());
        assertEquals(2, response.approved());
        assertEquals(1, response.declined());
        assertEquals(mockTransactions, response.transactions());
        assertTrue(response.elapsedMs() > 0);

        verify(cardProvider).getCards(3);
        verify(scenarioProvider).getScenario(ScenarioType.grocery);
        verify(merchantProvider).getMerchant(null, mockScenario);
    }



    @Test
    void cardProviderUsesRequestCount() {
        SimulatorRequest request = new SimulatorRequest(4, ScenarioType.grocery, null);
        Scenario mockScenario = mock(Scenario.class);
        List<Merchant> mockMerchants = List.of(mock(Merchant.class));
        SimulatorStats mockStats = mock(SimulatorStats.class);

        when(cardProvider.getCards(4)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(null, mockScenario)).thenReturn(mockMerchants);
        when(transactionBuilder.build(eq(4), any(), any(), any())).thenReturn(List.of());
        when(transactionSender.sendAll(any())).thenReturn(mockStats);

        SimulatorResponse respone = simulationService.run(request);

        assertEquals(4, respone.totalSubmitted());
        verify(cardProvider).getCards(4);
    }

    @Test
    void scenarioMccReachRepository() {
        SimulatorRequest request = new SimulatorRequest(1, ScenarioType.travel, null);
        Scenario mockScenario = mock(Scenario.class);
        List<Merchant> mockMerchants = List.of(mock(Merchant.class));
        SimulatorStats mockStats = mock(SimulatorStats.class);

        when(cardProvider.getCards(1)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.travel)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(null, mockScenario)).thenReturn(mockMerchants);
        when(transactionBuilder.build(eq(1), any(), any(), any())).thenReturn(List.of());

        when(transactionSender.sendAll(any())).thenReturn(mockStats);

        simulationService.run(request);

        verify(scenarioProvider).getScenario(ScenarioType.travel);
        verify(merchantProvider).getMerchant(null, mockScenario);
    }

    @Test
    void gatewayFailureDegradesToDeclined() {
        SimulatorRequest request = new SimulatorRequest(2, ScenarioType.grocery, null);
        Scenario mockScenario = mock(Scenario.class);

        when(cardProvider.getCards(2)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);

        when(merchantProvider.getMerchant(any(), any())).thenThrow(new RuntimeException("gateway down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> simulationService.run(request));
        assertEquals("gateway down", exception.getMessage());
    }

    @Test
    void noMerchants_throws() {
        SimulatorRequest request = new SimulatorRequest(1, ScenarioType.grocery, null);
        Scenario mockScenario = mock(Scenario.class);

        when(cardProvider.getCards(1)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(any(), any())).thenThrow(new IllegalArgumentException("Merchants with given mcc not found"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> simulationService.run(request));
        assertEquals("Merchants with given mcc not found", exception.getMessage());
    }

    @Test
    void noCards_throws() {
        SimulatorRequest request = new SimulatorRequest(1, ScenarioType.grocery, null);

        when(cardProvider.getCards(1)).thenThrow(new IllegalArgumentException("Cards not found"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> simulationService.run(request));
        assertEquals("Cards not found", exception.getMessage());
    }
}
