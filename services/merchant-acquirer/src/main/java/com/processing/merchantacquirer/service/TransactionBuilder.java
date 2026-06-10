package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import com.processing.common.dto.authorization.AuthorizationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionBuilder {
  private final AuthorizationRequestFactory authorizationRequestFactory;
  private final AcquirerProvider acquirerProvider;
  private final Random random = new Random();

  public List<AuthorizationRequest> build(
      int count,
      List<CardDataResponse> cardDataResponses,
      List<Merchant> merchants,
      Terminal terminal,
      Scenario scenario) {
    List<AuthorizationRequest> requests = new ArrayList<>(count);

    for (int i = 0; i < count; i++) {
      CardDataResponse card = cardDataResponses.get(i % cardDataResponses.size());
      Merchant merchant = merchants.get(random.nextInt(merchants.size()));
      Long amount = random.nextLong(scenario.getCountLower(), scenario.getCountUpper());

      AuthorizationRequest authorizationRequest = authorizationRequestFactory.build(
              card.pan(), card.currencyCode(), amount, terminal, merchant);
      requests.add(authorizationRequest);
      acquirerProvider.calculateFee(merchant, amount, authorizationRequest.stan(), card.pan());
    }

    return requests;
  }
}
