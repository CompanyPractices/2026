package com.processing.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.dto.AuthorizationRequest;
import com.processing.dto.AuthorizationResponse;
import com.processing.dto.CardResponse;
import com.processing.dto.ReserveRequest;
import com.processing.enums.CardStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.smartcardio.CardException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final HttpClient httpClient;

    @Value("${card-management.url}")
    private String cmsUrl;

    private final ObjectMapper objectMapper;

    public CardResponse getCard(String pan) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan))
                .GET()
                .build();

        HttpResponse<String> cardResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(cardResponse.body(), CardResponse.class);
    }

    public AuthorizationResponse authorize(AuthorizationRequest request) {
        CardResponse cardResponse;
        try {
            cardResponse = getCard(request.getPan());
        } catch (Exception e) {
            log.error("getting card from card managment service failed for pan: {}", request.getPan(), e.getMessage());
            return AuthorizationResponse.declined(request, "SERVICE_UNAVAILABLE", "96");
        }

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
        try {
            reserve(request.getAmount(), rrn, request.getPan());
        } catch (Exception e) {
            log.error("reserving failed for card {}",cardResponse.getId(), e.getMessage());
            return AuthorizationResponse.declined(request, "RESERVATION_FAILED", "96");
        }
        
        String authCode = generateAuthCode();
        return AuthorizationResponse.approved(request, rrn, authCode);
    }

    public void reserve(Integer amount, String rrn, String pan) throws Exception {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        String requestBody = objectMapper.writeValueAsString(reserveRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan + "/reserve"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new CardException("Failed to reserve. Status: " + response.statusCode());
        }
    }

    private final AtomicReference<String> lastTimestampAndSeq = new AtomicReference<>("");

    public String generateRRN() {
        Calendar calendar = Calendar.getInstance();

        String currentSecond = String.format("%1d%03d%02d%02d%02d",
                calendar.get(Calendar.YEAR) % 10,
                calendar.get(Calendar.DAY_OF_YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));

        String nextValue;
        while (true) {
            String currentState = lastTimestampAndSeq.get();
            int nextSeq = 0;
            if (currentState != null && currentState.startsWith(currentSecond)) {
                int lastSeq = Integer.parseInt(currentState.substring(10));
                nextSeq = (lastSeq + 1) % 100;
            }

            nextValue = currentSecond + String.format("%02d", nextSeq);
            if (lastTimestampAndSeq.compareAndSet(currentState, nextValue)) {
                break;
            }
        }
        return nextValue;
    }

    public String generateAuthCode() {
        return new Random().ints(6, 0, 36)
                .mapToObj(i -> Character.toString(i < 10 ? '0' + i : 'A' + i - 10))
                .collect(Collectors.joining());
    }
}
