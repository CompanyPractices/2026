package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import com.processing.common.dto.authorization.AuthorizationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionBuilder {
  private final AuthorizationRequestFactory authorizationRequestFactory;

  public List<AuthorizationRequest> build(
      int count,
      List<CardDataResponse> cardDataResponses,
      List<Merchant> merchants,
      Terminal terminal,
      Scenario scenario) {
    List<AuthorizationRequest> requests = new ArrayList<>(count);

    for (int i = 0; i < count; i++) {
      CardDataResponse card = cardDataResponses.get(i % cardDataResponses.size());
      Merchant merchant = merchants.get(ThreadLocalRandom.current().nextInt(merchants.size()));

      // Логика вычисления цены: берется поле "Средний чек мерчанта", задается диапазон [avg * 0.5, avg * 2].
      // Из данного диапазона рандом выбирает значение и проверяется попадание в диапазон цен сценария,
      // если выше, то берется верхняя отмета цены Сценария, если ниже, то нижняя отметка сценария
      // если попал в промежуток - то сгенерированное рандомом число.
      BigDecimal amount = new BigDecimal(
              Math.clamp(
                      ThreadLocalRandom.current().nextDouble(
                      merchant.getAverageCheck().doubleValue() * 0.5,
                      merchant.getAverageCheck().doubleValue() * 2),
              scenario.getCountLower().doubleValue(),
              scenario.getCountUpper().doubleValue()
      )).setScale(0, RoundingMode.HALF_EVEN);

      AuthorizationRequest authorizationRequest = authorizationRequestFactory.build(
              card.pan(), card.currencyCode(), amount, terminal, merchant);
      requests.add(authorizationRequest);
      log.info(String.valueOf(authorizationRequest));
    }

    return requests;
  }
}
