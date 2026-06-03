package com.processing.service;

import com.processing.dto.AuthorizationResponse;
import com.processing.dto.RunResponse;
import com.processing.dto.AuthorizationRequest;
import com.processing.model.Card;
import com.processing.model.TerminalType;
import com.processing.model.CardStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.processing.model.CardStatus.ACTIVE;

@Service
public class TerminalSimulatorService {

    private final Random random = new Random();
    private int stanCounter = 1;
    private List<Card> cards = List.of(new Card[]{new Card(1, "1", "1", "name", "1",
            ACTIVE, "1", 1, 1, 1, "1", "1")});

    private String getNextStan() {
        int stan = stanCounter;
        stanCounter++;
        if (stanCounter > 999999) {
            stanCounter = 1;
        }
        return String.format("%06d", stan);
    }

    private String getInvalidPan() {
        String validPan = getRandomCard(ACTIVE).getPan();
        char last = validPan.charAt(validPan.length() - 1);
        char newLast = (last == 0) ? '1' : '0';
        return validPan.substring(0, validPan.length() - 1) + newLast;
    }

    private Card getRandomCard(CardStatus cardStatus) {
        List<Card> filtered = cards.stream()
                .filter(c -> c.getStatus() == null || c.getStatus() == cardStatus)
                .toList();
        return filtered.get(random.nextInt(filtered.size()));
    }

    private AuthorizationRequest createTransaction(String scenario) {
        Card card;
        String mti = "0100";
        String stan = getNextStan();
        String processingCode = "000000";  // TODO: random/const?
        long amount;
        String transmissionDateTime;
        String terminalId = String.format("TERM%03d", ThreadLocalRandom.current().nextInt(1, 1000));
        String terminalType = String.valueOf(TerminalType.values()[(int)(Math.random()*3)]);
        String merchantId = "MERCH12345678901";  // TODO: random/const?
        String mcc = new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
        String acquirerId = String.format("TERM%03d", ThreadLocalRandom.current().nextInt(1, 1000));  // TODO: random/const?
        String issuerId = "";

        switch (scenario) {
            case "normal" -> { // TODO: random day time
                card = getRandomCard(ACTIVE);
                amount = 10_000 + (long) (Math.random() * 490_000);
                mcc = "5411";
                transmissionDateTime = "2026-06-01T14:30:00Z";
            }
            case "high_value" -> { // TODO: random day time
                card = getRandomCard(ACTIVE);
                amount = 10_000_000 + (long) (Math.random() * 40_000_000);
                transmissionDateTime = "2026-06-01T12:17:00Z";
            }
            default -> {  // TODO: random day/night time
                card = getRandomCard(ACTIVE);
                amount = 10_000 + (long) (Math.random() * 490_000);
                transmissionDateTime = "2026-06-01T14:30:00Z";
            }
        }

        String pan = card.getPan();
        String currencyCode = card.getCurrencyCode();

        return new AuthorizationRequest(mti, stan, pan, processingCode, amount, currencyCode, transmissionDateTime,
                terminalId, terminalType, merchantId, mcc, acquirerId, issuerId);
    }

    public RunResponse run(int count, String scenario) {
        long start = System.currentTimeMillis();
        getCardsFromCardManager();
        List<AuthorizationResponse> authResps = new ArrayList<>();
        int approved = 0, declined = 0;

        switch (scenario) {
            case "mixed" -> { // TODO
            }
            case "declines_test"-> { // TODO
            }
            case "night_time" ->  // TODO: random night time
            {
                //  AuthorizationRequest tx = createTransaction(norm, high);
                //  tx.setTransmissionDateTime("2026-06-01T03:50:00Z");
            }
            case "normal", "high_value" -> {
                for (int i = 0; i < count; i++) {
                    AuthorizationRequest tx = createTransaction(scenario);
                    AuthorizationResponse authResp = sendToGateway(tx);
                    authResps.add(authResp);
                    System.out.println(tx);

                    if ("APPROVED".equals(authResp.getStatus())) approved++;
                    else declined++;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        return new RunResponse(count, approved, declined, elapsed, authResps);
    }

    private AuthorizationResponse sendToGateway(AuthorizationRequest tx) {
        RestTemplate rest = new RestTemplate();
        String gatewayUrl = "http://gateway:8080/api/transactions";
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

    private void getCardsFromCardManager() {
        // TODO
    }
}
