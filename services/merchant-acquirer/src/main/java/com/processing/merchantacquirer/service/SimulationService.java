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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class SimulationService {
    private final MerchantRepository merchantRepository;
    private final GatewayClient gatewayClient;

    private final Map<String, Scenario> scenarios = Map.of(
            "grocery", new Scenario(List.of("5411", "5499"), 100, 3000, "8:00", "23:00", 95),
            "electronics", new Scenario(List.of("5732", "5045"), 5000, 100000, "10:00", "22:00", 90)
    );

    public SimulatorResponse run(SimulatorRequest request){
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
        if(request.getMccCodes() == null){
            merchants = merchantRepository.findByMccIn(scenario.getMcc());
            log.info(String.valueOf(merchants));
        }else{
            merchants = merchantRepository.findByMccIn(request.getMccCodes());
            log.info(String.valueOf(merchants));
        }
        log.info(String.valueOf(merchants));

        // Создание терминала
        Terminal terminal = new Terminal("TERM001", "POS");

        // Создание транакций
        List<AuthorizationRequest> authorizationRequests = new ArrayList<>();
        AuthorizationRequestFactory authorizationRequestFactory = new AuthorizationRequestFactory();
        for(CardDataResponse card : cardsResponse.cards()){
            AuthorizationRequest authorizationRequest = authorizationRequestFactory.build(
                    "0100",
                    card.pan(),
                    100,
                    terminal,
                    merchants.getFirst());

            authorizationRequests.add(authorizationRequest);
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
                authorizationResponses.add(new AuthorizationResponse("", " ", " ", " ", " ", " ", "999", 999));
                declined += 1;
            }
        }

        // Формирование ответа
        SimulatorResponse simulatorResponse = new SimulatorResponse(
                authorizationResponses.size(),
                approved,
                declined,
                2222,
                authorizationResponses
        );
        return simulatorResponse;
    }
}
