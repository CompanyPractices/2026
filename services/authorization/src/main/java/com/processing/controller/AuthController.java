package com.processing.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import com.processing.enums.CardStatus;
import com.processing.dto.*;
import java.lang.Exception;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private HttpClient httpClient = HttpClient.newHttpClient();

    private final String cmsUrl = "http://localhost:3000";

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

        reserve();
        String rnn = generateRNN();
        String authCode = generateAuthCode();
        return AuthorizationResponse.approved(request, rnn, authCode);
    }

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
                LocalDate.parse("2026-06-01T10:00:00"));
    }

    void reserve() {
        // TODO
    }

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static String lastSecond = "";

    private String generateRNN() {
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