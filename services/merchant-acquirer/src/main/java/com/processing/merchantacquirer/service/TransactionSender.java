package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
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

  public SimulatorStats sendAll(List<AuthorizationRequest> requests) {
    List<AuthorizationResponse> responses = new ArrayList<>(requests.size());

    int approved = 0;
    int declined = 0;

      for (Future<AuthorizationResponse> future : futures) {
        AuthorizationResponse response;
        try {
          response = future.get();
          transactionMetrics.record(request.mcc(), response.status());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while waiting for gateway", e);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof ExternalServiceException ese) {
            throw ese;
          }
          throw new IllegalStateException("Task filed", e.getCause());
        }

        responses.add(response);
        if ("APPROVED".equals(response.status())) {
          approved++;
        } else if ("DECLINED".equals(response.status())) {
          declined++;
        }
      }
      return new SimulatorStats(responses, approved, declined);
    }
  }
}
