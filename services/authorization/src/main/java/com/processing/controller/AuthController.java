package com.processing.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import com.processing.enums.CardStatus;
import com.processing.dto.*;
import org.springframework.web.bind.annotation.PostMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.Exception;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String cmsUrl = "http://localhost:8081";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/api/internal/authorize")
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

        String rrn = generateRRN();
        reserve(request.getAmount(), rrn, request.getPan());
        String authCode = generateAuthCode();
        return AuthorizationResponse.approved(request, rrn, authCode);
    }

    private CardResponse getCard(String pan) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan))
                .GET()
                .build();

        HttpResponse<String> cardResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(cardResponse.body(), CardResponse.class);
    }

    @PostMapping("/api/cards/{pan}/reserve")
    private boolean reserve(Integer amount, String rrn, String pan) throws Exception {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        String requestBody = objectMapper.writeValueAsString(reserveRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan + "/reserve"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static String lastSecond = "";

    private String generateRRN() {
        Calendar calendar = Calendar.getInstance();

        String currentSecond = String.format("%1d%03d%02d%02d%02d",
                calendar.get(Calendar.YEAR) % 10,
                calendar.get(Calendar.DAY_OF_YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));

        if (!currentSecond.equals(lastSecond)) {
            lastSecond = currentSecond;
            counter.set(0);
        }

        int sequence = counter.getAndIncrement() % 10;
        return currentSecond + sequence;
    }

    private String generateAuthCode() {
        return new Random().ints(6, 0, 36)
                .mapToObj(i -> Character.toString(i < 10 ? '0' + i : 'A' + i - 10))
                .collect(Collectors.joining());
    }
}