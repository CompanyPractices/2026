package com.processing.terminalsimulator.util;

import org.springframework.stereotype.Component;

@Component
public class StanGenerator {
    private int stanCounter = 1;
    private static int MAX_STAN_COUNT = 999999;

    public String getNextStan() {
        int stan = stanCounter;
        stanCounter++;
        if (stanCounter > MAX_STAN_COUNT) {
            stanCounter = 1;
        }
        return String.format("%06d", stan);
    }

}
