package com.processing.client;

import com.processing.dto.AuthorizationRequest;
import com.processing.dto.AuthorizationResponse;
import com.processing.dto.Card;
import com.processing.dto.CardsManagementResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class GatewayClient {
    private final RestTemplate rest = new RestTemplate();
    private final String gatewayUrl = "http://gateway:8080/api/transactions";
    private final String cardManagementUrl = "http://gateway:8080/api/cards?limit=200";

    public AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        try {
            ResponseEntity<AuthorizationResponse> response = rest.postForEntity(gatewayUrl, tx, AuthorizationResponse.class);
            return response.getBody();
        } catch (Exception e) {
            AuthorizationResponse errorResponse = new AuthorizationResponse();
            errorResponse.setStatus("DECLINED");
            errorResponse.setResponseCode("505");
            errorResponse.setDeclineReason(e.getMessage());
            return errorResponse;
        }
    }

    public List<Card> getCardsFromCardManager() {  // TODO: кидать ошибки
        try {
            ResponseEntity<CardsManagementResponse> response = rest.getForEntity(cardManagementUrl, CardsManagementResponse.class);
            CardsManagementResponse resp = response.getBody();
            return resp.cards();
        } catch (Exception e) {
            System.out.println("Cards from CardManager: " + e.getMessage());
            return null;
        }
    }

}
