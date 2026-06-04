package com.processing.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.dto.CardResponse;
import com.processing.dto.ReserveRequest;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AuthService {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${card-management.url}")
    private String cmsUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CardResponse getCard(String pan) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan))
                .GET()
                .build();

        HttpResponse<String> cardResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(cardResponse.body(), CardResponse.class);
    }

    public boolean reserve(Integer amount, String rrn, String pan) throws Exception {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        String requestBody = objectMapper.writeValueAsString(reserveRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cmsUrl + "/api/cards/" + pan + "/reserve"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
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
