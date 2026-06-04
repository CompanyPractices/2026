package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.client.GatewayClient;
import com.processing.merchantacquirer.client.dto.CardDataResponse;
import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.factory.AuthorizationRequestFactory;
import com.processing.merchantacquirer.domain.model.AuthorizationRequest;
import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import com.processing.merchantacquirer.controller.dto.*;
import com.processing.merchantacquirer.repository.MerchantRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@AllArgsConstructor
public class SimulationService {
    private final MerchantRepository merchantRepository;
    private final GatewayClient gatewayClient;
    private final AuthorizationRequestFactory authorizationRequestFactory;

    private final Map<String, Scenario> scenarios = Map.of(
            "grocery", new Scenario(List.of("5411", "5499"), 100, 3000, "8:00", "23:00", 95),
            "electronics", new Scenario(List.of("5732", "5045"), 5000, 100000, "10:00", "22:00", 90),
            "restaurant", new Scenario(List.of("5812", "5814"), 500, 5000, "11:00", "01:00", 90),
            "travel", new Scenario(List.of("3501", "4511", "4722"), 5000, 50000, "00:00", "24:00", 90)
    );

    public SimulatorResponse run(SimulatorRequest request){
        LocalDateTime startTime = LocalDateTime.now();
        log.info(String.valueOf(startTime));
        log.info(String.valueOf(request));
        // Получение карт (пока замокано на получение 1 захаркоженной карточки)
        CardsRequest cardsRequest = new CardsRequest(request.getCount(), 0, null, null);
//        CardsResponse cardsResponse = gatewayClient.getCards(cardsRequest);
        CardsResponse cardsResponse = new CardsResponse(1, List.of(new CardDataResponse("123", "1234567891234567", "123456", "IVAN VANYA", "0404", "ACTIVE", "643", "1", "2", "3", "123", "123")));
        log.info(String.valueOf(cardsResponse));

        // Получение сценария
        Scenario scenario = scenarios.get(request.getScenario());
        log.info(String.valueOf(scenario));

        // Получение мерчантов
        List<Merchant> merchants;
        int countMerchants;
        if(request.getMccCodes() == null){
            merchants = merchantRepository.findByMccIn(scenario.getMcc());
            log.info(String.valueOf(merchants));
            countMerchants = merchants.size();
        }else{
            merchants = merchantRepository.findByMccIn(request.getMccCodes());
            log.info(String.valueOf(merchants));
            countMerchants = merchants.size();
        }
        log.info(String.valueOf(merchants));

        // Создание терминала
        Terminal terminal = new Terminal("TERM001", "POS");

        // Создание транакций
        List<AuthorizationRequest> authorizationRequests = new ArrayList<>();
        if(cardsResponse.cards().size() < request.getCount()){
            int count = request.getCount();
            int iterableCard = cardsResponse.cards().size();
            log.info(String.valueOf(iterableCard));
            log.info(String.valueOf(count));
            Random random = new Random();
            while(count > 0){
                CardDataResponse card = cardsResponse.cards().get(iterableCard - 1);
                Merchant merchant = merchants.get(random.nextInt(0, countMerchants - 1));

                // Генерация цены
                int amount = random.nextInt(scenario.getCountLower(), scenario.getCountUpper());

                AuthorizationRequest authorizationRequest = authorizationRequestFactory.build(
                        card.pan(),
                        amount,
                        terminal,
                        merchant);
                log.info(String.valueOf(authorizationRequest));
                authorizationRequests.add(authorizationRequest);

                iterableCard--;
                count--;
                if(iterableCard == 0){
                    iterableCard = cardsResponse.cards().size();
                }
            }
        }

        // Отправка транзакций
        int approved = 0;
        int declined = 0;

        List<AuthorizationResponse> authorizationResponses = new ArrayList<>();
        for(AuthorizationRequest transaction: authorizationRequests){
            try {
                AuthorizationResponse response = gatewayClient.processAuthorize(transaction);
                authorizationResponses.add(response);
                approved += 1;
            } catch (Exception e) {
                authorizationResponses.add(new AuthorizationResponse("0100", transaction.getStan(), null, null, "505", "DECLINED", e.toString(), 999));
                declined += 1;
            }
        }

        // Формирование
        LocalDateTime endTime = LocalDateTime.now();
        log.info(String.valueOf(endTime));
        SimulatorResponse simulatorResponse = new SimulatorResponse(
                authorizationResponses.size(),
                approved,
                declined,
                (int) Duration.between(startTime, endTime).toMillis(),
                authorizationResponses
        );
        return simulatorResponse;
    }
}
