package com.processing.merchantacquirer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.controller.dto.SimulatorRequest;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.ScenarioType;
import com.processing.merchantacquirer.domain.model.AuthorizationRequest;
import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
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
    void run_endToEndAcrossRealBeans() {
        SimulatorRequest request = new SimulatorRequest(3, ScenarioType.grocery, null);
        List<CardDataResponse> mockCards = List.of(mock(CardDataResponse.class), mock(CardDataResponse.class));
        Scenario mockScenario = mock(Scenario.class);
        List<Merchant> mockMerchants = List.of(mock(Merchant.class));
        List<AuthorizationRequest> mockAuthRequests = List.of(
                mock(AuthorizationRequest.class),
                mock(AuthorizationRequest.class),
                mock(AuthorizationRequest.class)
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
        when(transactionBuilder.build(anyInt(), any(), any(), any(), any()))
                .thenReturn(mockAuthRequests);
        when(transactionSender.sendAll(any())).thenReturn(mockStats);

        SimulatorResponse response = simulationService.run(request);

        assertThat(response.totalSubmitted()).isEqualTo(3);
        assertThat(response.approved()).isEqualTo(2);
        assertThat(response.declined()).isEqualTo(1);
        assertThat(response.transactions()).hasSize(3);
        assertThat(response.elapsedMs()).isGreaterThanOrEqualTo(0);

        verify(cardProvider).getCards(3);
        verify(scenarioProvider).getScenario(ScenarioType.grocery);
        verify(merchantProvider).getMerchant(null, mockScenario);
    }



    @Test
    void run_cardProviderUsesRequestCount() {
        SimulatorRequest request = new SimulatorRequest(4, ScenarioType.grocery, null);
        Scenario mockScenario = mock(Scenario.class);
        List<Merchant> mockMerchants = List.of(mock(Merchant.class));
        SimulatorStats mockStats = mock(SimulatorStats.class);

        when(cardProvider.getCards(4)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(null, mockScenario)).thenReturn(mockMerchants);
        when(transactionBuilder.build(eq(4), any(), any(), any(), any())).thenReturn(List.of());
        when(transactionSender.sendAll(any())).thenReturn(mockStats);

        simulationService.run(request);

        verify(cardProvider).getCards(4);
    }

    @Test
    void run_scenarioMccReachRepository() {
        SimulatorRequest request = new SimulatorRequest(1, ScenarioType.travel, null);
        Scenario mockScenario = mock(Scenario.class);
        List<Merchant> mockMerchants = List.of(mock(Merchant.class));
        SimulatorStats mockStats = mock(SimulatorStats.class);

        when(cardProvider.getCards(1)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.travel)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(null, mockScenario)).thenReturn(mockMerchants);
        when(transactionBuilder.build(eq(1), any(), any(), any(), any())).thenReturn(List.of());

        when(transactionSender.sendAll(any())).thenReturn(mockStats);

        simulationService.run(request);

        verify(scenarioProvider).getScenario(ScenarioType.travel);
        verify(merchantProvider).getMerchant(null, mockScenario);
    }

    @Test
    void run_gatewayFailureDegradesToDeclined() {
        SimulatorRequest request = new SimulatorRequest(2, ScenarioType.grocery, null);
        Scenario mockScenario = mock(Scenario.class);

        when(cardProvider.getCards(2)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);

        when(merchantProvider.getMerchant(any(), any())).thenThrow(new RuntimeException("gateway down"));

        assertThatThrownBy(() -> simulationService.run(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("gateway down");
    }

    @Test
    void run_noMerchants_throws() {
        SimulatorRequest request = new SimulatorRequest(1, ScenarioType.grocery, null);
        Scenario mockScenario = mock(Scenario.class);

        when(cardProvider.getCards(1)).thenReturn(List.of());
        when(scenarioProvider.getScenario(ScenarioType.grocery)).thenReturn(mockScenario);
        when(merchantProvider.getMerchant(any(), any())).thenThrow(new IllegalArgumentException("Merchants with given mcc not found"));

        assertThatThrownBy(() -> simulationService.run(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchants with given mcc");
    }

    @Test
    void run_noCards_throws() {
        SimulatorRequest request = new SimulatorRequest(1, ScenarioType.grocery, null);

        when(cardProvider.getCards(1)).thenThrow(new IllegalArgumentException("Cards not found"));

        assertThatThrownBy(() -> simulationService.run(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cards not found");
    }
}
