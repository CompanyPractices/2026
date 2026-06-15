package com.processing.terminalsimulator.client;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.cardmanagement.GetCardsResponse;
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
        this.gatewayUrl = gatewayUrl + "/api/transactions";
        this.cardManagementUrl = cardManagementUrl + "/api/cards";
    }

    public AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        ResponseEntity<AuthorizationResponse> response = rest.postForEntity(gatewayUrl,
                tx, AuthorizationResponse.class);
        return response.getBody();
    }

    public List<CardModel> getCardsFromCardManager(CardModelStatus status, int amount) {
        String fullUrl = UriComponentsBuilder.fromUriString(cardManagementUrl)
                .queryParam("status", status)
                .queryParam("limit", amount != 0 ? amount : null)
                .build()
                .toUriString();
        ResponseEntity<GetCardsResponse> response = rest.getForEntity(fullUrl, GetCardsResponse.class);
        GetCardsResponse resp = response.getBody();
        if (resp == null || resp.total() == 0 || resp.cards().isEmpty()) {
            throw new IllegalStateException("No " + status + " cards available");
        }
        return resp.cards();
    }

}
