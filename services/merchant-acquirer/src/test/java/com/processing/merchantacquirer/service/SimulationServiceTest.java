package com.processing.merchantacquirer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import com.processing.merchantacquirer.controller.dto.SimulatorRequest;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.ScenarioType;
import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import com.processing.merchantacquirer.repository.MerchantRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SimulationServiceTest {

  @Autowired private SimulationService simulationService;

  @MockitoBean private GatewayClient gatewayClient;

  @MockitoBean private MerchantRepository merchantRepository;

  private Merchant merchant(String id, String mcc) {
    Merchant m = new Merchant();
    m.setId(id);
    m.setMcc(mcc);
    m.setAcquirerId("ACQ-1");
    m.setAcquiringFee(150);
    m.setAverageCheck(1000L);
    return m;
  }

  private CardsResponse cards(int n) {
    CardDataResponse[] arr = new CardDataResponse[n];
    for (int i = 0; i < n; i++) {
      arr[i] =
          new CardDataResponse(
              "c" + i,
              "411111111111111" + i,
              "411111",
              "IVAN",
              "0404",
              "ACTIVE",
              "643",
              "1",
              "2",
              "3",
              "iss",
              "now");
    }
    return new CardsResponse(n, List.of(arr));
  }

  private AuthorizationResponse approved() {
    return new AuthorizationResponse("0110", "x", "rrn", "auth", "000", "APPROVED", null, 12);
  }

  private AuthorizationResponse declined() {
    return new AuthorizationResponse("0110", "x", "rrn", null, "100", "DECLINED", "low", 12);
  }

  @Test
  void run_endToEndAcrossRealBeans() {
    when(gatewayClient.getCards(any())).thenReturn(cards(2));
    when(merchantRepository.findByMccIn(any()))
        .thenReturn(List.of(merchant("M-1", "5411"), merchant("M-2", "5499")));
    when(gatewayClient.processAuthorize(any())).thenReturn(approved(), declined(), approved());

    SimulatorResponse response =
        simulationService.run(new SimulatorRequest(3, ScenarioType.grocery, null));

    assertThat(response.totalSubmitted()).isEqualTo(3);
    assertThat(response.approved()).isEqualTo(2);
    assertThat(response.declined()).isEqualTo(1);
    assertThat(response.transactions()).hasSize(3);
    assertThat(response.elapsedMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void run_cardProviderUsesRequestCount() {
    when(gatewayClient.getCards(any())).thenReturn(cards(1));
    when(merchantRepository.findByMccIn(any()))
        .thenReturn(List.of(merchant("M-1", "5411"), merchant("M-2", "5499")));
    when(gatewayClient.processAuthorize(any())).thenReturn(approved());

    simulationService.run(new SimulatorRequest(4, ScenarioType.grocery, null));

    org.mockito.Mockito.verify(gatewayClient).getCards(argThat((CardsRequest r) -> r.limit() == 4));
  }

  @Test
  void run_scenarioMccReachRepository() {
    when(gatewayClient.getCards(any())).thenReturn(cards(1));
    when(merchantRepository.findByMccIn(any()))
        .thenReturn(List.of(merchant("M-1", "3501"), merchant("M-2", "4511")));
    when(gatewayClient.processAuthorize(any())).thenReturn(approved());

    simulationService.run(new SimulatorRequest(1, ScenarioType.travel, null));

    org.mockito.Mockito.verify(merchantRepository)
        .findByMccIn(argThat(c -> c.containsAll(List.of("3501", "4511", "4722"))));
  }

  @Test
  void run_gatewayFailureDegradesToDeclined() {
    when(gatewayClient.getCards(any())).thenReturn(cards(1));
    when(merchantRepository.findByMccIn(any()))
        .thenReturn(List.of(merchant("M-1", "5411"), merchant("M-2", "5499")));
    when(gatewayClient.processAuthorize(any())).thenThrow(new RuntimeException("gateway down"));

    SimulatorResponse response =
        simulationService.run(new SimulatorRequest(2, ScenarioType.grocery, null));

    assertThat(response.declined()).isEqualTo(2);
    assertThat(response.approved()).isZero();
    assertThat(response.transactions())
        .allSatisfy(tx -> assertThat(tx.responseCode()).isEqualTo("505"));
  }

  @Test
  void run_noMerchants_throws() {
    when(gatewayClient.getCards(any())).thenReturn(cards(1));
    when(merchantRepository.findByMccIn(any())).thenReturn(List.of());

    assertThatThrownBy(
            () -> simulationService.run(new SimulatorRequest(1, ScenarioType.grocery, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Merchants with given mcc ([5411, 5499]) not found");
  }

  @Test
  void run_noCards_throws() {
    when(gatewayClient.getCards(any())).thenReturn(new CardsResponse(0, List.of()));
    when(merchantRepository.findByMccIn(any())).thenReturn(List.of(merchant("M-1", "5411")));

    assertThatThrownBy(
            () -> simulationService.run(new SimulatorRequest(1, ScenarioType.grocery, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cards not found");
  }
}
