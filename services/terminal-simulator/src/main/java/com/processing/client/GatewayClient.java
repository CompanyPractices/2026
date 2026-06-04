package com.processing.client;

import com.processing.dto.AuthorizationRequest;
import com.processing.dto.AuthorizationResponse;
import com.processing.dto.Card;
import com.processing.dto.CardsManagementResponse;
import com.processing.model.CardStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class GatewayClient {
    private final RestTemplate rest = new RestTemplate();
    private final String gatewayUrl = "http://gateway:8080/api/transactions";
    private final String cardManagementUrl = "http://gateway:8080/api/cards";

    public AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        try {
            ResponseEntity<AuthorizationResponse> response = rest.postForEntity(gatewayUrl, tx, AuthorizationResponse.class);
            return response.getBody();
        } catch (Exception e) {  // TODO: кидать ошибку
            AuthorizationResponse errorResponse = new AuthorizationResponse();
            errorResponse.setStatus("DECLINED");
            errorResponse.setResponseCode("505");
            errorResponse.setDeclineReason(e.getMessage());
            return errorResponse;
        }
    }

    public List<Card> getCardsFromCardManager(CardStatus status, int amount) {
        try {
            String fullUrl = UriComponentsBuilder.fromUriString(cardManagementUrl)
                    .queryParam("status", status)
                    .queryParam("limit", amount != 0 ? amount : null)
                    .build()
                    .toUriString();
            ResponseEntity<CardsManagementResponse> response = rest.getForEntity(fullUrl, CardsManagementResponse.class);
            CardsManagementResponse resp = response.getBody();
            if (resp.total() == 0) {   // TODO: кидать ошибку мало карт
            }
            return resp.cards();
        } catch (Exception e) {  // TODO: кидать ошибку
            System.out.println("Error CardManager: " + e.getMessage());
            return null;
        }
    }

}
