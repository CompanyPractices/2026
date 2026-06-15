package com.processing.terminalsimulator.util;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class StanGenerator {
    private final AtomicInteger stanCounter = new AtomicInteger(0);

    public String getNextStan() {
        int stan = stanCounter.addAndGet(1);
        if (stan > 999999) {
            stanCounter.set(1);
            stan = stanCounter.get();
        }
        return String.format("%06d", stan);
    }

}
