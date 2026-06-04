package com.processing.service;

import com.processing.client.GatewayClient;
import com.processing.dto.AuthorizationResponse;
import com.processing.dto.RunResponse;
import com.processing.dto.AuthorizationRequest;
import com.processing.dto.Card;
import com.processing.model.TerminalType;
import com.processing.model.CardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.processing.model.CardStatus.ACTIVE;
import static com.processing.model.CardStatus.BLOCKED;

@Service
@RequiredArgsConstructor
public class TerminalSimulatorService {
    private final GatewayClient gatewayClient;

    private final Random random = new Random();
    private int stanCounter = 1;
    private List<Card> cards = List.of(new Card[]{new Card(1, "1", "1", "name", "1",
            ACTIVE, "1", 15, 35, 50, "1", "1"),
            new Card(1, "2", "2", "name2", "2",
                    BLOCKED, "2", 23, 123, 140, "2", "2")});

    private static String randomDateTime(String timeOfDay) {
        int year = 2026;
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, 28);

        int hour;
        if ("night".equals(timeOfDay)) {
            hour = ThreadLocalRandom.current().nextInt(1, 5);
        } else {
            hour = ThreadLocalRandom.current().nextInt(9, 22);
        }
        int minute = ThreadLocalRandom.current().nextInt(0, 60);
        int second = ThreadLocalRandom.current().nextInt(0, 60);

        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second);
        return ldt.toInstant(ZoneOffset.UTC).toString();
    }

    private String getNextStan() {
        int stan = stanCounter;
        stanCounter++;
        if (stanCounter > 999999) {
            stanCounter = 1;
        }
        return String.format("%06d", stan);
    }

    private String getInvalidPan() {
        String validPan = getRandomCard(ACTIVE).pan();
        char last = validPan.charAt(validPan.length() - 1);
        char newLast = (last == 0) ? '1' : '0';
        return validPan.substring(0, validPan.length() - 1) + newLast;
    }

    private Card getRandomCard(CardStatus cardStatus) {
        List<Card> filtered = cards.stream()
                .filter(c -> c.status() == null || c.status() == cardStatus)
                .toList();
        return filtered.get(random.nextInt(filtered.size()));
    }

    private AuthorizationRequest createTransaction(String scenario, String partOfDay) {
        Card card;
        String mti = "0100";
        String stan = getNextStan();
        String processingCode = "000000";
        String transmissionDateTime = randomDateTime(partOfDay);
        String terminalId = String.format("TERM%03d", ThreadLocalRandom.current().nextInt(1, 1000));
        String terminalType = String.valueOf(TerminalType.values()[(int)(Math.random()*3)]);
        String merchantId = "MERCH12345678901";
        String mcc = new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
        String acquirerId = String.format("TERM%03d", ThreadLocalRandom.current().nextInt(1, 1000));
        String issuerId = "";
        card = getRandomCard(ACTIVE);
        long amount = (long)(Math.random() * 2_000_000);

        switch (scenario) {
            case "normal" -> {
                amount = 10_000 + (long)(Math.random() * 490_000);
                mcc = "5411";
            }
            case "high_value" -> amount = 10_000_000 + (long) (Math.random() * 40_000_000);
            case "daily_limit" -> amount = card.dailyLimit() - 1;
            case "blocked" -> card = getRandomCard(BLOCKED);
            case "no_money" -> amount = card.availableBalance() + (int)(Math.random()*100000);
            case "more_day_limit" -> amount = card.dailyLimit() + (int)(Math.random()*10000);
        }

        String pan = card.pan();
        if (scenario.equals("invalid_pan")) {
            pan = getInvalidPan();
        }
        String currencyCode = card.currencyCode();

        return new AuthorizationRequest(mti, stan, pan, processingCode, amount, currencyCode, transmissionDateTime,
                terminalId, terminalType, merchantId, mcc, acquirerId, issuerId);
    }

    private void handler(int start, int end, AtomicInteger approved, AtomicInteger declined,
                         String scenario, List<AuthorizationResponse> authResps, String partOfDay) {
        for (int i = start; i < end; i++) {
            AuthorizationRequest tx = createTransaction(scenario, partOfDay);
            AuthorizationResponse authResp = gatewayClient.sendToGateway(tx);
            authResps.add(authResp);
            System.out.println(tx);

            if ("APPROVED".equals(authResp.getStatus())) {
                approved.incrementAndGet();
            }
            else declined.incrementAndGet();
        }
    }

    public RunResponse run(int count, String scenario) {
        long start = System.currentTimeMillis();
        List<Card> cardManagementCards = gatewayClient.getCardsFromCardManager();
        cards = (cardManagementCards != null) ? cardManagementCards : cards;   // TODO: как заработает модуль gateway
                                                                              // - убрать хардкод карт
        List<AuthorizationResponse> authResps = new ArrayList<>();
        AtomicInteger approved = new AtomicInteger(0), declined = new AtomicInteger(0);

        switch (scenario) {
            case "mixed" -> {
                handler(0, (int)(count * 0.7), approved, declined, "normal", authResps, "day");
                handler((int)(count * 0.7), (int)(count * 0.7 + count * 0.15), approved, declined,
                        "high_value", authResps, "day");
                handler((int)(count * 0.7 + count * 0.15), (int)(count * 0.7 + count * 0.15 + count * 0.1),
                        approved, declined, "daily_limit", authResps, "day");
                handler((int)(count * 0.7 + count * 0.15 + count * 0.1), count, approved, declined,
                        "blocked", authResps, "day");
            }
            case "declines_test"-> {
                handler(0, (int)(count * 0.2), approved, declined, "invalid_pan", authResps,
                        "day");
                handler((int)(count * 0.2), (int)(count * 0.4), approved, declined, "blocked", authResps,
                        "day");
                handler((int)(count * 0.4), (int)(count * 0.6), approved, declined, "no_money", authResps,
                        "day");
                handler((int)(count * 0.6), (int)(count * 0.8), approved, declined, "more_day_limit",
                        authResps, "day");
                handler((int)(count * 0.8), count, approved, declined, "normal", authResps, "day");
            }
            case "night_time" -> {
                handler(0, count/2, approved, declined, "normal", authResps, "night");
                handler(count/2, count, approved, declined, "high_value", authResps, "night");
            }
            case "normal", "high_value" -> handler(0, count, approved, declined, scenario, authResps, "day");
        }

        long elapsed = System.currentTimeMillis() - start;
        return new RunResponse(count, approved.get(), declined.get(), elapsed, authResps);
    }
}
