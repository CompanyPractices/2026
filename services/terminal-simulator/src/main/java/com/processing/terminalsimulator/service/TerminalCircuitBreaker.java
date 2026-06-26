package com.processing.terminalsimulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class TerminalCircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int windowSize;
    private final double failureRateThreshold;
    private final long cooldownMs = 3000;
    private final int successTestThreshold = 2;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private volatile long lastStateChangeTime = 0;

    private final boolean[] window;
    private int head = 0;
    private int currentErrors = 0;
    private int totalCalls = 0; // Заполненность окна

    private final ReentrantLock lock = new ReentrantLock();

    private int halfOpenSuccesses = 0;

    public TerminalCircuitBreaker(
            @Value("${simulator.cb.windowSize:10}") int windowSize,
            @Value("${simulator.cb.failureRate:0.2}") double failureRateThreshold) {
        this.windowSize = windowSize;
        this.failureRateThreshold = failureRateThreshold; // например, 0.5 = 50%
        this.window = new boolean[windowSize];
    }

    public boolean isCallAllowed() {
        if (state.get() == State.OPEN) {
            if (System.currentTimeMillis() - lastStateChangeTime > cooldownMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("Circuit Breaker: Entering HALF_OPEN. Testing gateway...");
                    lock.lock();
                    try {
                        halfOpenSuccesses = 0;
                    } finally {
                        lock.unlock();
                    }
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        processResult(false, null);
    }

    public void recordNetworkFailure(String reason) {
        processResult(true, reason);
    }

    private void processResult(boolean isError, String reason) {
        lock.lock();
        try {
            State currentState = state.get();

            if (currentState == State.OPEN) {
                return;
            }

            if (currentState == State.HALF_OPEN) {
                if (isError) {
                    if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                        log.error("Circuit Breaker: HALF_OPEN test failed ({}). Back to OPEN", reason);
                        lastStateChangeTime = System.currentTimeMillis();
                    }
                } else {
                    halfOpenSuccesses++;
                    if (halfOpenSuccesses >= successTestThreshold) {
                        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                            log.info("Circuit Breaker: CLOSED. Gateway fully recovered!");
                            resetWindow();
                        }
                    }
                }
                return;
            }

            if (currentState == State.CLOSED) {

                if (totalCalls == windowSize) {
                    if (window[head]) {
                        currentErrors--;
                    }
                } else {
                    totalCalls++;
                }

                window[head] = isError;
                if (isError) {
                    currentErrors++;
                }

                head = (head + 1) % windowSize;

                if (totalCalls == windowSize) {
                    double currentFailureRate = (double) currentErrors / windowSize;

                    if (currentFailureRate >= failureRateThreshold) {
                        if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                            log.error("Circuit Breaker: OPEN! Failure rate {}% ({} errors out of {}). Disabling network!",
                                    Math.round(currentFailureRate * 100), currentErrors, windowSize);
                            lastStateChangeTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void resetWindow() {
        head = 0;
        currentErrors = 0;
        totalCalls = 0;
    }
}
