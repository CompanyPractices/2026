package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.metrics.TransactionMetrics;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSender {
  private final GatewayClient gatewayClient;
  private final TransactionMetrics transactionMetrics;
  private final AcquirerProvider acquirerProvider;

  public SimulatorStats sendAll(List<AuthorizationRequest> requests) {
    List<AuthorizationResponse> responses = new ArrayList<>(requests.size());

    int approved = 0;
    int declined = 0;

    for (AuthorizationRequest request : requests) {
      acquirerProvider.calculateFee(
              request.merchantId(), request.amount(), request.transmissionDateTime(),
              request.stan(), request.terminalId(), request.pan());
      AuthorizationResponse response;
      try {
        response = gatewayClient.processAuthorize(request);
      } catch (Exception e) {
        response =
            new AuthorizationResponse(
                "0100", request.stan(), null, null, "505", "DECLINED", e.getMessage(), 999);
      }
      log.info("AuthorizationResponse: {} ", response);

      transactionMetrics.record(request.mcc(), response.status());

      responses.add(response);

      if (response.status().equals("APPROVED")) {
        approved++;
      }
      if (response.status().equals("DECLINED")) {
        declined++;
      }
    }
    return new SimulatorStats(responses, approved, declined);
  }
}
