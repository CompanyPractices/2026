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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class TerminalSimulatorService {
    private final GatewayClient gatewayClient;
    private final TransactionFactory transactionFactory;
    private final int tps;

    public  TerminalSimulatorService(GatewayClient gatewayClient, TransactionFactory transactionFactory,
                                     @Value("${simulator.tps:100}") int tps) {
        this.gatewayClient = gatewayClient;
        this.transactionFactory = transactionFactory;
        this.tps = tps;
    }

    private record TransactionTask(TransactionType type, PartofDay partOfDay) {}

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

    private List<CardModel> loadCards(TerminalScenario scenario) {
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
        return newCards;
    }

    private AuthorizationResponse executeSingleTransaction(TransactionType transactionType, PartofDay partOfDay,
                                                           List<CardModel> cards, AtomicInteger approved,
                                                           AtomicInteger declined,
                                                           String terminalId) {
        CardModelStatus requiredStatus = transactionFactory.getRequiredStatus(transactionType);
        CardModel card = getRandomCard(requiredStatus, cards);
        AuthorizationRequest tx = transactionFactory.create(transactionType, partOfDay, card, terminalId);
        AuthorizationResponse authResp = gatewayClient.sendToGateway(tx);

        if (TransactionStatus.APPROVED.name().equals(authResp.status())) {
            approved.incrementAndGet();
        } else if (TransactionStatus.DECLINED.name().equals(authResp.status())) {
            declined.incrementAndGet();
        }
        return authResp;
    }

    private void addTask(List<TransactionTask> tasks, TransactionType type, int start, int end, PartofDay partOfDay) {
        for (int i = start; i < end; i++) {
            tasks.add(new TransactionTask(type, partOfDay));
        }
    }

    private List<TransactionTask> generateTasks(TerminalScenario scenario, int count) {
        List<TransactionTask> tasks = new ArrayList<>();

        switch (scenario) {
            case mixed -> {
                addTask(tasks, TransactionType.NORMAL, 0, (int) (count * 0.7), PartofDay.DAY);
                addTask(tasks, TransactionType.HIGH_VALUE, (int) (count * 0.7), (int) (count * 0.7 + count * 0.15),
                        PartofDay.DAY);
                addTask(tasks, TransactionType.ALMOST_DAILY_LIMIT, (int) (count * 0.7 + count * 0.15),
                        (int) (count * 0.7 + count * 0.15 + count * 0.1), PartofDay.DAY);
                addTask(tasks, TransactionType.BLOCKED, (int) (count * 0.7 + count * 0.15 + count * 0.1), count,
                        PartofDay.DAY);
            }
            case declines_test -> {
                addTask(tasks, TransactionType.INVALID_PAN, 0, (int) (count * 0.2), PartofDay.DAY);
                addTask(tasks, TransactionType.BLOCKED, (int) (count * 0.2), (int) (count * 0.4), PartofDay.DAY);
                addTask(tasks, TransactionType.NO_MONEY, (int) (count * 0.4), (int) (count * 0.6), PartofDay.DAY);
                addTask(tasks, TransactionType.MORE_THAN_DAILY_LIMIT, (int) (count * 0.6), (int) (count * 0.8),
                        PartofDay.DAY);
                addTask(tasks, TransactionType.NORMAL, (int) (count * 0.8), count, PartofDay.DAY);
            }
            case night_time -> {
                addTask(tasks, TransactionType.NORMAL, 0, count / 2, PartofDay.NIGHT);
                addTask(tasks, TransactionType.HIGH_VALUE, count / 2, count, PartofDay.NIGHT);
            }
            case normal -> addTask(tasks,  TransactionType.NORMAL, 0, count, PartofDay.DAY);
            case high_value -> addTask(tasks, TransactionType.HIGH_VALUE, 0, count, PartofDay.DAY);
        }
        return tasks;
    }

    public TerminalRunResponse run(int count, TerminalScenario scenario) {
        long start = System.currentTimeMillis();
        String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
        List<CardModel> cards = loadCards(scenario);
        List<TransactionTask> tasks = generateTasks(scenario, count);

        ConcurrentLinkedQueue<AuthorizationResponse> authResponses = new ConcurrentLinkedQueue<>();
        AtomicInteger approved = new AtomicInteger(0);
        AtomicInteger declined = new AtomicInteger(0);
        long delayMs = 1000 / tps;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // главный поток заходит и раздает задачи экзекутору
            for (TransactionTask task : tasks) {
                // неблокирующий метод, закидывает задачу в пул
                executor.submit(
                        () -> {  // создаю задачу (объект Runnable), без входящих аргументов, с таким кодом:
                    try {
                        AuthorizationResponse resp = executeSingleTransaction(task.type, task.partOfDay, cards,
                                approved, declined, terminalId);
                        authResponses.add(resp);
                    } catch (Exception e) {
                        log.error("Transaction failed", e);
                        authResponses.add(new AuthorizationResponse(null, null, null, null, null,
                                null, e.getMessage(), 0));
                    }
                });

                Thread.sleep(delayMs);
            }
            // тут вызывается executor.close() и ждет завершения всех задач(!!) без allOf().join()
        } catch (Exception e) {
            log.error("Execution error", e);
            authResponses.add(new AuthorizationResponse(null, null, null, null, null,
                    null, e.getMessage(), 0));
        }

        long elapsed = System.currentTimeMillis() - start;
        return new TerminalRunResponse(count, approved.get(), declined.get(), elapsed, authResponses.stream().toList());
    }
}
