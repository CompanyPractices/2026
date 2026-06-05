package com.processing.authorization.services;

import com.processing.authorization.dto.AuthorizationRequest;
import com.processing.authorization.dto.AuthorizationResponse;
import com.processing.authorization.dto.CardResponse;
import com.processing.authorization.dto.ReserveRequest;
import com.processing.authorization.enums.CardStatus;
import com.processing.authorization.exceptions.CardNotFoundException;
import com.processing.authorization.exceptions.ReserveCardException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final RestTemplate restTemplate;

    @Value("${card-management.url}")
    private String cmsUrl;

    public AuthorizationResponse authorize(AuthorizationRequest request) {
        CardResponse cardResponse;
        try {
            cardResponse = getCard(request.getPan());
        } catch (CardNotFoundException e) {
            log.debug("Card not found for pan: {}", request.getPan());
            return AuthorizationResponse.declined(request, "CARD_NOT_FOUND", "14");
        } catch (Exception e) {
            log.debug("getting card from card managment service failed for pan: {}", request.getPan(), e);
            return AuthorizationResponse.declined(request, "SERVICE_UNAVAILABLE", "96");
        }

        CardStatus currCardStatus = cardResponse.getStatus();
        if (currCardStatus == null) {
            return AuthorizationResponse.declined(request, "UNKNOWN_REASON", "05");
        }
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
            log.debug("reserving failed for card {}", cardResponse.getId(), e);
            return AuthorizationResponse.declined(request, "RESERVATION_FAILED", "96");
        }

        String authCode = generateAuthCode();
        return AuthorizationResponse.approved(request, rrn, authCode);
    }

    public CardResponse getCard(String pan) throws Exception {
        String url = cmsUrl + "/api/cards/" + pan;
        log.debug("Getting card info for pan {}", pan);
        ResponseEntity<CardResponse> response = restTemplate.getForEntity(url, CardResponse.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.debug("Failed to get card. Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to get card. Status: " + response.getStatusCode());
        }
        return response.getBody();
    }

    public void reserve(Integer amount, String rrn, String pan) throws Exception {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReserveRequest> requestEntity = new HttpEntity<>(reserveRequest, headers);
        String url = cmsUrl + "/api/cards/" + pan + "/reserve";
        log.debug("Reserving amount {} for card {} with rrn {}", amount, pan, rrn);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.debug("Reserve failed. Status: {}", response.getStatusCode());
            throw new ReserveCardException("Failed to reserve. Status: " + response.getStatusCode());
        }
        log.debug("Reserve successful for card {}", pan);
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
