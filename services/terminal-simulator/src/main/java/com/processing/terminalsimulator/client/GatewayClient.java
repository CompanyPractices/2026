package com.processing.terminalsimulator.client;

import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.dto.CardsManagementResponse;
import com.processing.terminalsimulator.model.CardStatus;
import com.processing.terminalsimulator.dto.AuthorizationRequest;
import com.processing.terminalsimulator.dto.AuthorizationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class GatewayClient {
    private final RestTemplate rest;
    private final String gatewayUrl;
    private final String cardManagementUrl;

    public GatewayClient(RestTemplate rest,
                         @Value("${gateway.url}") String gatewayUrl,
                         @Value("${gateway.card-management-url}") String cardManagementUrl) {
        this.rest = rest;
        this.gatewayUrl = gatewayUrl;
        this.cardManagementUrl = cardManagementUrl;
    }

    public AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        ResponseEntity<AuthorizationResponse> response = rest.postForEntity(gatewayUrl, tx, AuthorizationResponse.class);
        return response.getBody();
    }

    public List<Card> getCardsFromCardManager(CardStatus status, int amount) {
        String fullUrl = UriComponentsBuilder.fromUriString(cardManagementUrl)
                .queryParam("status", status)
                .queryParam("limit", amount != 0 ? amount : null)
                .build()
                .toUriString();
        ResponseEntity<CardsManagementResponse> response = rest.getForEntity(fullUrl, CardsManagementResponse.class);
        CardsManagementResponse resp = response.getBody();
        if (resp == null || resp.total() == 0 || resp.cards().isEmpty()) {
            throw new IllegalStateException("No " + status + " cards available");
        }
        return resp.cards();
    }

}
