package com.processing.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import com.processing.enums.CardStatus;
import com.processing.dto.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.Exception;
import java.util.UUID;

public class AuthController {

    private HttpClient httpClient = HttpClient.newHttpClient();

    private final String cmsUrl = "http://localhost:8081";

    public AuthorizationResponse authorize(AuthorizationRequest request) throws Exception {
        CardResponse cardResponse = getCard(request.getPan());

        CardStatus currCardStatus = cardResponse.getStatus();
        if (!currCardStatus.equals(CardStatus.ACTIVE)) {
            return switch (currCardStatus) {
                case EXPIRED -> AuthorizationResponse.declined(request, "CARD_EXPIRED", "54");
                case BLOCKED -> AuthorizationResponse.declined(request, "CARD_BLOCKED", "05");
                case INACTIVE -> AuthorizationResponse.declined(request, "CARD_INACTIVE", "05");
                default -> AuthorizationResponse.declined(request, "UNKNOWN_REASON", "05");
            };
        }


         if (LocalDate.parse(cardResponse.getExpiryDate()).isBefore(LocalDate.now())) {
             return AuthorizationResponse.declined(request, "CARD_EXPIRED", "54");
         }

         // TODO check daily limit when add table limit_usage

        // TODO check month limit when add table limit_usage

        if (request.getAmount() > cardResponse.getAvailableBalance())
            return AuthorizationResponse.declined(request, "INSUFFICIENT_FUNDS", "51");

        reserve(request.getAmount(), request.getPan()); // rrn
        generateRnn();
        generateAuthCode();
        return AuthorizationResponse.approved(request, "", ""); // Заглушка
    }

    @GetMapping("/api/cards/{pan}")
    private CardResponse getCard(String pan) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(cmsUrl + "/api/cards/" + pan))
            .GET()
            .build();
 
        HttpResponse<String> cardResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
 
        // заглушка
        return new CardResponse(
            UUID.randomUUID(), 
            "1234123412341234", 
            "400000",
            "IVAN IVANOV",
            "1228",
            CardStatus.ACTIVE,
            "643",
            15000000,
            300000000,
            100000000,
            "ISS001",
            LocalDate.parse("2026-06-01T10:00:00")
        );
    }

    @PostMapping("/api/cards/{pan}/reserve")
    void reserve(Integer amount, String pan) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan + "/reserve"))
                .POST(HttpRequest.BodyPublishers.)
                .build();

        HttpResponse<String> cardResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    }

    void generateRnn() {
        // TODO
    }

    void generateAuthCode() {
        // TODO
    }
}