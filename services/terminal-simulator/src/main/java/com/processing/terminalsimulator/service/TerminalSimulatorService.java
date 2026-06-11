package com.processing.terminalsimulator.service;

import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.dto.AuthorizationResponse;
import com.processing.terminalsimulator.dto.RunResponse;
import com.processing.terminalsimulator.dto.AuthorizationRequest;
import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.factory.TransactionFactory;
import com.processing.terminalsimulator.model.Scenario;
import com.processing.terminalsimulator.model.CardStatus;
import com.processing.terminalsimulator.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.processing.terminalsimulator.model.CardStatus.ACTIVE;
import static com.processing.terminalsimulator.model.CardStatus.BLOCKED;

@Service
@RequiredArgsConstructor
public class TerminalSimulatorService {
    private final GatewayClient gatewayClient;
    private final TransactionFactory transactionFactory;

    private final Random random = new Random();
    private volatile List<Card> cards = new ArrayList<>();

    private Card getRandomCard(CardStatus cardStatus) {
        List<Card> filtered = cards.stream()
                .filter(c -> cardStatus == null || c.status() == cardStatus)
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalStateException("No " + cardStatus + " cards available");
        }
        return filtered.get(random.nextInt(filtered.size()));
    }

    private void generateTransactionHandler(int start, int end, AtomicInteger approved, AtomicInteger declined,
                                            TransactionType transactionType, List<AuthorizationResponse> authResps,
                                            String partOfDay) {
        CardStatus requiredStatus = transactionFactory.getRequiredStatus(transactionType);
        for (int i = start; i < end; i++) {
            Card card = getRandomCard(requiredStatus);
            AuthorizationRequest tx = transactionFactory.create(transactionType, partOfDay, card);
            AuthorizationResponse authResp = gatewayClient.sendToGateway(tx);
            authResps.add(authResp);

            if ("APPROVED".equals(authResp.status())) {
                approved.incrementAndGet();
            } else if ("DECLINED".equals(authResp.status())) {
                declined.incrementAndGet();
            }
        }
    }

    public RunResponse run(int count, Scenario scenario) {
        long start = System.currentTimeMillis();
        List<Card> activeCards = gatewayClient.getCardsFromCardManager(ACTIVE, 70);
        if (activeCards == null || activeCards.isEmpty()) {
            throw new IllegalStateException("No ACTIVE cards available");
        }
        List<Card> newCards = new ArrayList<>(activeCards);
        boolean needBlocked = scenario == Scenario.mixed || scenario == Scenario.declines_test;
        if (needBlocked) {
            List<Card> blockedCards = gatewayClient.getCardsFromCardManager(BLOCKED, 30);
            if (blockedCards == null || blockedCards.isEmpty()) {
                throw new IllegalStateException("No BLOCKED cards available");
            }
            newCards.addAll(blockedCards);
        }
        cards = newCards;

        List<AuthorizationResponse> authResps = new ArrayList<>();
        AtomicInteger approved = new AtomicInteger(0);
        AtomicInteger declined = new AtomicInteger(0);

        switch (scenario) {
            case mixed -> {
                generateTransactionHandler(0, (int) (count * 0.7), approved, declined, TransactionType.NORMAL,
                        authResps, "day");
                generateTransactionHandler((int) (count * 0.7), (int) (count * 0.7 + count * 0.15), approved, declined,
                        TransactionType.HIGH_VALUE, authResps, "day");
                generateTransactionHandler((int) (count * 0.7 + count * 0.15),
                        (int) (count * 0.7 + count * 0.15 + count * 0.1), approved, declined,
                        TransactionType.ALMOST_DAILY_LIMIT, authResps, "day");
                generateTransactionHandler((int) (count * 0.7 + count * 0.15 + count * 0.1), count, approved, declined,
                        TransactionType.BLOCKED, authResps, "day");
            }
            case declines_test -> {
                generateTransactionHandler(0, (int) (count * 0.2), approved, declined,
                        TransactionType.INVALID_PAN, authResps, "day");
                generateTransactionHandler((int) (count * 0.2), (int) (count * 0.4), approved, declined,
                        TransactionType.BLOCKED, authResps, "day");
                generateTransactionHandler((int) (count * 0.4), (int) (count * 0.6), approved, declined,
                        TransactionType.NO_MONEY, authResps, "day");
                generateTransactionHandler((int) (count * 0.6), (int) (count * 0.8), approved, declined,
                        TransactionType.MORE_THAN_DAILY_LIMIT, authResps, "day");
                generateTransactionHandler((int) (count * 0.8), count, approved, declined,
                        TransactionType.NORMAL, authResps, "day");
            }
            case night_time -> {
                generateTransactionHandler(0, count / 2, approved, declined, TransactionType.NORMAL,
                        authResps, "night");
                generateTransactionHandler(count / 2, count, approved, declined, TransactionType.HIGH_VALUE,
                        authResps, "night");
            }
            case normal -> generateTransactionHandler(0, count, approved, declined, TransactionType.NORMAL,
                    authResps, "day");
            case high_value -> generateTransactionHandler(0, count, approved, declined, TransactionType.HIGH_VALUE,
                    authResps, "day");
        }

        long elapsed = System.currentTimeMillis() - start;
        return new RunResponse(count, approved.get(), declined.get(), elapsed, authResps);
    }
}
