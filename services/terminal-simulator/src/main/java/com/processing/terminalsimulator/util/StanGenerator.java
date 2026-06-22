package com.processing.terminalsimulator.util;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StanGenerator {
    private final Map<String, AtomicInteger> terminalStans = new ConcurrentHashMap<>();
    private static final int MAX_STAN_COUNT = 999_999;

    public String getNextStan(String terminalId) {
        AtomicInteger stanCounter = terminalStans.computeIfAbsent(terminalId, k -> new AtomicInteger(0));

        int stan = stanCounter.incrementAndGet();

        // TODO: race
        if (stan > MAX_STAN_COUNT) {
            stanCounter.set(1);
            stan = 1;
        }

        return String.format("%06d", stan);
    }

}
