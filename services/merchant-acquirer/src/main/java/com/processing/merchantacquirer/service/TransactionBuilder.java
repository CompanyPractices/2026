package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import com.processing.common.dto.authorization.AuthorizationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.processing.merchantacquirer.repository.AcquirerFeeRepository;
import com.processing.merchantacquirer.service.dto.RequestFeeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionBuilder {
  private final AuthorizationRequestFactory authorizationRequestFactory;
  private final AcquirerProvider acquirerProvider;
  private final TerminalProvider terminalProvider;
  private final AcquirerFeeRepository acquirerFeeRepository;
  private final Random random = new Random();

  public List<AuthorizationRequest> build(
      int count,
      List<CardDataResponse> cardDataResponses,
      List<Merchant> merchants,
      Scenario scenario) {

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<RequestFeeData>> futures = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
        int finalI = i;
        Future<RequestFeeData> future = executor.submit(
                () -> {
                  CardDataResponse card = cardDataResponses.get(finalI % cardDataResponses.size());
                  Merchant merchant = merchants.get(random.nextInt(merchants.size()));
                  Terminal terminal = terminalProvider.getByMerchant(merchant.getId());
                  Long amount = random.nextLong(scenario.getCountLower(), scenario.getCountUpper());

                  AuthorizationRequest authorizationRequest = authorizationRequestFactory.build(
                          card.pan(), card.currencyCode(), amount, terminal, merchant);

                  log.info(String.valueOf(authorizationRequest));
                  AcquirerFee fee = acquirerProvider.calculateFee(
                          merchant, authorizationRequest.amount(), authorizationRequest.transmissionDateTime(),
                          authorizationRequest.stan(), authorizationRequest.terminalId(), authorizationRequest.pan());
                  return new RequestFeeData(authorizationRequest, fee);
                }
                );
        futures.add(future);
      }

      List<AuthorizationRequest> requests = new ArrayList<>(count);
      List<AcquirerFee> fees = new ArrayList<>(count);
      for (Future<RequestFeeData> future : futures) {
        try {
          requests.add(future.get().authorizationRequest());
          fees.add(future.get().fee());
        } catch (ExecutionException e) {
          throw new IllegalStateException("Failed build authorization request", e.getCause());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while build auhtorization request", e);
        }
      }
      acquirerFeeRepository.saveAll(fees);
      return requests;
    }
  }
}
