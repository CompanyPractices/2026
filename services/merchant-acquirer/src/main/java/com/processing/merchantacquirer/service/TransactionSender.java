package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.merchantacquirer.metrics.TransactionMetrics;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSender {
  private final GatewayClient gatewayClient;
  private final TransactionMetrics transactionMetrics;
  private final int concurency = 24;
  private final Semaphore semaphore = new Semaphore(concurency);

  public SimulatorStats sendAll(List<AuthorizationRequest> requests) {

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<AuthorizationResponse>> futures = new ArrayList<>(requests.size());

      for (AuthorizationRequest request : requests) {
        semaphore.acquire();
        try {
          futures.add(executor.submit(
                  () -> {
                    try {
                      AuthorizationResponse response = gatewayClient.processAuthorize(request);
                      transactionMetrics.record(request.mcc(), response.status());
                      log.info("AuthorizationResponse: {}", response);
                      return response;
                    } catch (ExternalServiceException e) {
                      transactionMetrics.record(request.mcc(), "ERROR");
                      log.warn("Gateway returned failed authorization {}: {}", request, e.getMessage());
                      return AuthorizationResponse.systemError(request.stan());
                    }
                  }
          ));
        } catch (RuntimeException e) {
          semaphore.release();
          throw e;
        }
      }

      List<AuthorizationResponse> responses = new ArrayList<>(requests.size());

      int approved = 0;
      int declined = 0;

      for (Future<AuthorizationResponse> future : futures) {
        AuthorizationResponse response;
        try {
          response = future.get();

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while waiting for gateway", e);
        } catch (ExecutionException e) {
          log.error("Unsuccessful authorization request: {}", e.getMessage());
          continue;
        }

        responses.add(response);
        if ("APPROVED".equals(response.status())) {
          approved++;
        } else if ("DECLINED".equals(response.status())) {
          declined++;
        }
      }
      return new SimulatorStats(responses, approved, declined);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
  }
}
