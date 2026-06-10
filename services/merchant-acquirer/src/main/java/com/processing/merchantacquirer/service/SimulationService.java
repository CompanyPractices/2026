package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.controller.dto.*;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.service.dto.SimulatorStats;
import com.processing.common.dto.authorization.AuthorizationRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SimulationService {
  public final CardProvider cardProvider;
  public final MerchantProvider merchantProvider;
  public final TransactionBuilder transactionBuilder;
  public final TransactionSender transactionSender;
  public final ScenarioProvider scenarioProvider;
  public final AcquirerProvider acquirerProvider;

  public SimulatorResponse run(SimulatorRequest request) {
    LocalDateTime startTime = LocalDateTime.now();
    log.info(String.valueOf(request));

    List<CardDataResponse> cards = cardProvider.getCards(request.count());
    log.info(String.valueOf(cards));

    // Получение сценария
    Scenario scenario = scenarioProvider.getScenario(request.scenario());
    log.info(String.valueOf(scenario));

    // Получение мерчантов
    List<Merchant> merchants = merchantProvider.getMerchant(request.mccCodes(), scenario);
    log.info(String.valueOf(merchants));

    // Создание терминала
    Terminal terminal = new Terminal("TERM" + ThreadLocalRandom.current().nextInt(1, 1000), "POS");

    // Создание транакций
    List<AuthorizationRequest> authorizationRequests =
        transactionBuilder.build(request.count(), cards, merchants, terminal, scenario);

    SimulatorStats stats = transactionSender.sendAll(authorizationRequests);

    // Формирование
    LocalDateTime endTime = LocalDateTime.now();

    return new SimulatorResponse(
        request.count(),
        stats.approved(),
        stats.declined(),
        (int) Duration.between(startTime, endTime).toMillis(),
        stats.responses());
  }

  public List<Merchant> getAllMerchants() {
    return merchantProvider.getAll();
  }

  public long countMerchants() {
    return merchantProvider.count();
  }

  public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
    return acquirerProvider.getAcquirerFee(request);
  }
}
