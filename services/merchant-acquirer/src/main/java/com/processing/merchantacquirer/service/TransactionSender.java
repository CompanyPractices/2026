package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.domain.MaskerPan;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.merchantacquirer.metrics.TransactionMetrics;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
                    .refillIntervally(tps, Duration.ofSeconds(1))
            )
            .build();
    log.info("Initialized with Concurrency: {} and Target TPS: {}", concurrency, tps);
  }

  public SimulatorStats sendAll(List<AuthorizationRequest> requests) {

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<AuthorizationResponse>> futures = new ArrayList<>(requests.size());

      for (AuthorizationRequest request : requests) {
        semaphore.acquire();
        try {
          futures.add(executor.submit(
                  () -> {
                    try {
                      bucket.asBlocking().consume(1);

                      AuthorizationResponse response = gatewayClient.processAuthorize(request);
                      transactionMetrics.record(request.mcc(), response.status());
                      if (response.status().equals("DECLINED")) {
                        log.error("AuthorizationResponse returned as declined: {}", response);
                      }

                      return response;
                    } catch (ExternalServiceException e) {
                      transactionMetrics.record(request.mcc(), "ERROR");

                      log.warn("Gateway returned failed AuthorizationRequest: [STAN: {}, PAN: {}, amount: {}, "
                                      + "TerminalID: {}, MerchantID: {}, AcquirerID: {}, MCC: {}]: {}",
                              request.stan(),
                              MaskerPan.mask(request.pan()),
                              request.amount(),
                              request.terminalId(),
                              request.merchantId(),
                              request.acquirerId(),
                              request.mcc(), e.getMessage());

                      return AuthorizationResponse.systemError(request.stan());
                    } finally {
                      semaphore.release();
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
