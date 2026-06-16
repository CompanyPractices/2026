package com.processing.terminalsimulator.service;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.common.dto.terminalsimulator.TerminalRunResponse;
import com.processing.common.dto.terminalsimulator.TerminalScenario;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import com.processing.terminalsimulator.factory.TransactionFactory;
import com.processing.terminalsimulator.client.GatewayClient;
import com.processing.terminalsimulator.model.PartofDay;
import com.processing.terminalsimulator.model.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalSimulatorService {
    private final GatewayClient gatewayClient;
    private final TransactionFactory transactionFactory;

    private CardModel getRandomCard(CardModelStatus cardStatus, List<CardModel> cards) {
        List<CardModel> filtered = cards.stream()
                .filter(c -> cardStatus == null || c.status() == cardStatus)
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalStateException("No " + cardStatus + " cards available");
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(filtered.size());
        return filtered.get(randomIndex);
    }

    private void generateTransactionHandler(int start, int end, AtomicInteger approved, AtomicInteger declined,
                                            TransactionType transactionType, ConcurrentLinkedQueue<AuthorizationResponse> authResps,
                                            PartofDay partOfDay, List<CardModel> cards) {
        CardModelStatus requiredStatus = transactionFactory.getRequiredStatus(transactionType);
        for (int i = start; i < end; i++) {
            try {
                CardModel card = getRandomCard(requiredStatus, cards);
                String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
                AuthorizationRequest tx = transactionFactory.create(transactionType, partOfDay, card, terminalId);
                AuthorizationResponse authResp = gatewayClient.sendToGateway(tx);
                authResps.add(authResp);

                if (TransactionStatus.APPROVED.name().equals(authResp.status())) {
                    approved.incrementAndGet();
                } else if (TransactionStatus.DECLINED.name().equals(authResp.status())) {
                    declined.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("Failed to process transaction {}/{} in range [{}-{}]", i, end, start, end, e);
                authResps.add(new AuthorizationResponse(null, null, null, null, null,
                        null, e.getMessage(), 0));

            }
        }
    }

    public TerminalRunResponse run(int count, TerminalScenario scenario) {
        List<CardModel> cards;

        long start = System.currentTimeMillis();
        List<CardModel> activeCards = gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE, 70);
        if (activeCards == null || activeCards.isEmpty()) {
            throw new IllegalStateException("No ACTIVE cards available");
        }
        List<CardModel> newCards = new ArrayList<>(activeCards);
        boolean needBlocked = scenario == TerminalScenario.mixed || scenario == TerminalScenario.declines_test;
        if (needBlocked) {
            List<CardModel> blockedCards = gatewayClient.getCardsFromCardManager(CardModelStatus.BLOCKED, 30);
            if (blockedCards == null || blockedCards.isEmpty()) {
                throw new IllegalStateException("No BLOCKED cards available");
            }
            newCards.addAll(blockedCards);
        }
        cards = newCards;

        ConcurrentLinkedQueue<AuthorizationResponse> authResps = new ConcurrentLinkedQueue<>();
        AtomicInteger approved = new AtomicInteger(0);
        AtomicInteger declined = new AtomicInteger(0);

        switch (scenario) {
            case mixed -> {
                generateTransactionHandler(0, (int) (count * 0.7), approved, declined, TransactionType.NORMAL,
                        authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.7), (int) (count * 0.7 + count * 0.15), approved, declined,
                        TransactionType.HIGH_VALUE, authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.7 + count * 0.15),
                        (int) (count * 0.7 + count * 0.15 + count * 0.1), approved, declined,
                        TransactionType.ALMOST_DAILY_LIMIT, authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.7 + count * 0.15 + count * 0.1), (int) count, approved, declined,
                        TransactionType.BLOCKED, authResps, PartofDay.DAY, cards);
            }
            case declines_test -> {
                generateTransactionHandler(0, (int) (count * 0.2), approved, declined,
                        TransactionType.INVALID_PAN, authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.2), (int) (count * 0.4), approved, declined,
                        TransactionType.BLOCKED, authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.4), (int) (count * 0.6), approved, declined,
                        TransactionType.NO_MONEY, authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.6), (int) (count * 0.8), approved, declined,
                        TransactionType.MORE_THAN_DAILY_LIMIT, authResps, PartofDay.DAY, cards);
                generateTransactionHandler((int) (count * 0.8), count, approved, declined,
                        TransactionType.NORMAL, authResps, PartofDay.DAY, cards);
            }
            case night_time -> {
                generateTransactionHandler(0, count / 2, approved, declined, TransactionType.NORMAL,
                        authResps, PartofDay.NIGHT, cards);
                generateTransactionHandler(count / 2, count, approved, declined, TransactionType.HIGH_VALUE,
                        authResps, PartofDay.NIGHT, cards);
            }
            case normal -> generateTransactionHandler(0, count, approved, declined, TransactionType.NORMAL,
                    authResps, PartofDay.DAY, cards);
            case high_value -> generateTransactionHandler(0, count, approved, declined, TransactionType.HIGH_VALUE,
                    authResps, PartofDay.DAY, cards);
        }

        long elapsed = System.currentTimeMillis() - start;
        return new TerminalRunResponse(count, approved.get(), declined.get(), elapsed, authResps.stream().toList());
    }
}
