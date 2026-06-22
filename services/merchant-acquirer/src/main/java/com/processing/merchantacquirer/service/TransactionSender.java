package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.merchantacquirer.metrics.TransactionMetrics;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransactionSender {
  private final GatewayClient gatewayClient;
  private final TransactionMetrics transactionMetrics;
  private final Semaphore semaphore;
  private final Bucket bucket;

  public TransactionSender(GatewayClient gatewayClient,
                           TransactionMetrics transactionMetrics,
                           @Value("${simulation.sender.concurrency}") int concurrency,
                           @Value("${simulation.sender.tps}") int tps) {
    this.gatewayClient = gatewayClient;
    this.transactionMetrics = transactionMetrics;
    this.semaphore = new Semaphore(concurrency);
    this.bucket = Bucket.builder()
            .addLimit(limit -> limit
                    .capacity(tps)
                    .refillGreedy(tps, Duration.ofSeconds(1))
            )
            .build();
    log.info("Initialized with Concurrency: {} and Target TPS: {}", concurrency, tps);
  }

  public SimulatorStats sendAll(List<AuthorizationRequest> requests) {

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<AuthorizationResponse>> futures = new ArrayList<>(requests.size());

      for (AuthorizationRequest request : requests) {
        futures.add(executor.submit(
                () -> {
                  try {
                    bucket.asBlocking().consume(1);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupet while whited rate limit", e);
                  }

                  try {
                    semaphore.acquire();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupet while acquiring sender permin", e);
                  }

                  try {
                    AuthorizationResponse response = gatewayClient.processAuthorize(request);
                    transactionMetrics.record(request.mcc(), response.status());

                    return response;
                  } catch (ExternalServiceException e) {
                    transactionMetrics.record(request.mcc(), "ERROR");

                    return AuthorizationResponse.systemError(request.stan());
                  } finally {
                    semaphore.release();
                  }
                }
        ));
      }

      List<AuthorizationResponse> responses = new ArrayList<>(requests.size());
      Map<String, Integer> declinedByReason = new HashMap<>();
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
          String reason = !response.responseCode().equals("00") ? response.declineReason() : "UNKNOWN";
          declinedByReason.merge(reason, 1, Integer::sum);
        }
      }

      if (declined > 0) {
        log.warn("Declined {} of {}. Stats: {}", declined, responses.size(), declinedByReason);
      }
      return new SimulatorStats(responses, approved, declined);
    }
  }
}
