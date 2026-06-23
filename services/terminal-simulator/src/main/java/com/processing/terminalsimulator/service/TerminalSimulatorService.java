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
import com.processing.common.dto.terminalsimulator.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Slf4j
@Service
public class TerminalSimulatorService {
    private final GatewayClient gatewayClient;
    private final TransactionFactory transactionFactory;
    private final int tps;
    private final int cardsAmount;
    private final TerminalCircuitBreaker circuitBreaker;

    private final AtomicBoolean isContinuousRunning = new AtomicBoolean(false);
    private final AtomicReference<Thread> continuousLoopThread = new AtomicReference<>(null);  // видимость из разных потоков (методы start-continuous и stop)

    public  TerminalSimulatorService(GatewayClient gatewayClient, TransactionFactory transactionFactory,
                                     TerminalCircuitBreaker circuitBreaker,
                                     @Value("${simulator.tps:100}") int tps,
                                     @Value("${simulator.cardsAmount:5000}") int cardsAmount) {
        this.gatewayClient = gatewayClient;
        this.transactionFactory = transactionFactory;
        this.circuitBreaker = circuitBreaker;
        this.tps = tps;
        this.cardsAmount = cardsAmount;
    }

    private record TransactionTask(TransactionType type, PartofDay partOfDay) {}

    private CardModel getRandomCard(CardModelStatus cardStatus, Map<CardModelStatus, List<CardModel>> cards) {
        List<CardModel> filtered = cards.get(cardStatus);
        if (filtered == null || filtered.isEmpty()) {
            throw new IllegalStateException("No " + cardStatus + " cards available");
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(filtered.size());
        return filtered.get(randomIndex);
    }

    private List<CardModel> loadCards(TerminalScenario scenario) {
        boolean needBlocked = scenario == TerminalScenario.mixed || scenario == TerminalScenario.declines_test;
        int blockedPercent = 20; // 20%
        int activePercent = (needBlocked) ? 80 : 100;

        List<CardModel> activeCards = gatewayClient.getCardsFromCardManager(CardModelStatus.ACTIVE,
                (cardsAmount * activePercent / 100));
        if (activeCards == null || activeCards.isEmpty()) {
            throw new IllegalStateException("No ACTIVE cards available");
        }
        List<CardModel> newCards = new ArrayList<>(activeCards);
        if (needBlocked) {
            List<CardModel> blockedCards = gatewayClient.getCardsFromCardManager(CardModelStatus.BLOCKED,
                    (cardsAmount * blockedPercent / 100));
            if (blockedCards == null || blockedCards.isEmpty()) {
                throw new IllegalStateException("No BLOCKED cards available");
            }
            newCards.addAll(blockedCards);
        }
        return newCards;
    }

    private AuthorizationResponse executeSingleTransaction(TransactionType transactionType, PartofDay partOfDay,
                                                           AtomicInteger approved,
                                                           AtomicInteger declined,
                                                           Map<CardModelStatus, List<CardModel>> cardsByStatus,
                                                           String terminalId) {
        CardModelStatus requiredStatus = transactionFactory.getRequiredStatus(transactionType);
        CardModel card = getRandomCard(requiredStatus, cardsByStatus);
        AuthorizationRequest tx = transactionFactory.create(transactionType, partOfDay, card, terminalId);
        AuthorizationResponse authResp = gatewayClient.sendToGateway(tx);

        if (TransactionStatus.APPROVED.name().equals(authResp.status())) {
            approved.incrementAndGet();
        } else if (TransactionStatus.DECLINED.name().equals(authResp.status())) {
            declined.incrementAndGet();
            if (!authResp.declineReason().equals("INSUFFICIENT_FUNDS") && !authResp.declineReason().equals("CARD_BLOCKED")
                    && !authResp.declineReason().equals("EXCEEDS_AMOUNT_LIMIT")
                    && !authResp.declineReason().equals("CARD_NOT_FOUND")) {
                log.warn("Terminal-simulator: declined transaction in HTTP response: {}",
                        authResp.declineReason());
            }
        } else {
            log.warn("Terminal-simulator: not accepted/declined transaction in HTTP response: {}",
                    authResp.declineReason());
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

    private TransactionTask generateSingleTask(TransactionType transactionType) {
        return new TransactionTask(transactionType, PartofDay.DAY);
    }

    public TerminalRunResponse run(int count, TerminalScenario scenario) {
        long start = System.currentTimeMillis();
        circuitBreaker.reset();
        Map<CardModelStatus, List<CardModel>> cardsByStatus;

        String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
        List<CardModel> cards = loadCards(scenario);
        cardsByStatus = cards.stream().collect(Collectors.groupingBy(CardModel::status));
        List<TransactionTask> tasks = generateTasks(scenario, count);

        ConcurrentLinkedQueue<AuthorizationResponse> authResponses = new ConcurrentLinkedQueue<>();
        AtomicInteger approved = new AtomicInteger(0);
        AtomicInteger declined = new AtomicInteger(0);

        AtomicInteger totalSubmitted = new AtomicInteger(0);
        long delayMs = 1000 / tps;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // главный поток заходит и раздает задачи экзекутору
            for (TransactionTask task : tasks) {
                // неблокирующий метод, закидывает задачу в пул
                executor.submit(
                        () -> {  // создаю задачу (объект Runnable), без входящих аргументов, с таким кодом:
                    totalSubmitted.incrementAndGet();

                    if (!circuitBreaker.isCallAllowed()) {
                        authResponses.add(new AuthorizationResponse(null, null, null, null,
                                null, null, "Circuit Breaker in transaction-simulator: "
                                + "Gateway is down, request skipped", 0));
                        return;
                    }
                    try {
                        AuthorizationResponse resp = executeSingleTransaction(task.type, task.partOfDay,
                                approved, declined, cardsByStatus, terminalId);
                        authResponses.add(resp);

                        circuitBreaker.recordSuccess();
                    } catch (org.springframework.web.client.ResourceAccessException e) {
                        handleNetworkFailure(e.getMessage(), authResponses);
                    } catch (org.springframework.web.client.HttpStatusCodeException e) {
                        if (e.getStatusCode().is5xxServerError()) {
                            handleNetworkFailure("Gateway internal error(5xx): " + e.getStatusCode(), authResponses);
                        } else {
                            handleInternalFailure("Gateway client error(4xx): " + e.getMessage(), e, authResponses);
                        }
                    } catch (Exception e) {
                        handleInternalFailure("Internal terminal simulation error", e, authResponses);
                    }
                });

                Thread.sleep(delayMs);
            }
            // тут вызывается executor.close() и ждет завершения всех задач(!!) без allOf().join()
        } catch (InterruptedException e) {  // Если кто-то решит прервать главный поток, то он вызовет у главного потока
            // метод .interrupt(), который поставит внутри потока флаг interrupted = true. Это увидит Thread.sleep(),
            // выкинет InterruptedException и поставит обратно interrupted = false. Блок for прервется, успев создать
            // только часть тасок.
            log.error("Simulation loop was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();  // возвращаем interrupted = true, чтобы для внешнего мира не выглядело
            // так, будто наш поток завершился сам, добровольно и успешно.
            // Все это делается, чтобы собрать остатки данных и вернуть хотя бы то, что успело выполниться, вместо
            // ошибки 500.
        } catch (Exception e) {
            log.error("Critical execution error in simulation main loop: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        return new TerminalRunResponse(totalSubmitted.get(), approved.get(), declined.get(), elapsed,
                authResponses.stream().toList());
    }

    public void startContinuous(int tps, TransactionType transactionType) {
        if (!isContinuousRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Continuous simulation is already running!");
        }
        log.info("Starting continuous simulation. TPS: {}, Scenario: {}", tps, transactionType);

        Map<CardModelStatus, List<CardModel>> cardsByStatus;
        long delayMs = 1000 / tps;
        String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
        CardModelStatus requiredStatus = transactionFactory.getRequiredStatus(transactionType);
        List<CardModel> cards = gatewayClient.getCardsFromCardManager(requiredStatus, cardsAmount);
        cardsByStatus = cards.stream().collect(Collectors.groupingBy(CardModel::status));

        if (cardsByStatus.isEmpty()) {
            isContinuousRunning.set(false);
            throw new IllegalStateException("No cards available for status: " + requiredStatus);
        }

        // хочу создать виртуальный поток, один-единственный фоновый поток-диспетчер
        Thread thread = Thread.ofVirtual().unstarted(() -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // когда я в методе stopContinuous() вызываю continuousLoopThread.interrupt(), я не убиваю поток,
                // а ставлю ему внутренний флаг interrupted=true
                while (isContinuousRunning.get() && !Thread.currentThread().isInterrupted()) {
                    TransactionTask task = generateSingleTask(transactionType);
                    executor.submit(() -> {
                        if (!circuitBreaker.isCallAllowed()) {
                            return;
                        }
                        try {
                            executeSingleTransaction(task.type, task.partOfDay, new AtomicInteger(),
                                    new AtomicInteger(), cardsByStatus, terminalId);
                            circuitBreaker.recordSuccess();
                        } catch (org.springframework.web.client.ResourceAccessException
                                 | org.springframework.web.client.HttpStatusCodeException e) {
                            circuitBreaker.recordNetworkFailure(e.getMessage());
                        } catch (Exception e) {
                            log.error("Continuous transaction execution failed", e);
                        }
                    });

                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException e) {
                log.info("Continuous simulation loop was interrupted: {}", e.getMessage());
            } finally {
                isContinuousRunning.set(false);
            }
        });

        continuousLoopThread.set(thread);
        thread.start();

    }

    public void stopContinuous() {
        if (isContinuousRunning.compareAndSet(true, false)) {
            Thread threadToInterrupt = continuousLoopThread.getAndSet(null);  // прерываю поток-диспетчер,
            // чтобы он вышел из Thread.sleep()
            if (threadToInterrupt != null) {
                threadToInterrupt.interrupt();
                try {
                    // жду, пока поток завершится и закроет свой экзекутор внутри try-with-resources
                    threadToInterrupt.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info("Continuous simulation stopped.");
            }
        }
    }

    private void handleInternalFailure(String contextMessage, Exception e,
                                       ConcurrentLinkedQueue<AuthorizationResponse> authResponses) {
        log.error("{}, but keeping simulation", contextMessage, e);
        authResponses.add(new AuthorizationResponse(null, null, null, null,
                null, null, "Internal simulation error: " + e.getMessage(), 0));
    }

    private void handleNetworkFailure(String errorMessage, ConcurrentLinkedQueue<AuthorizationResponse> authResponses) {
        log.warn("Network transaction failed: {}", errorMessage);
        circuitBreaker.recordNetworkFailure(errorMessage);

        authResponses.add(new AuthorizationResponse(null, null, null, null,
                null, null, errorMessage, 0));
    }
}
