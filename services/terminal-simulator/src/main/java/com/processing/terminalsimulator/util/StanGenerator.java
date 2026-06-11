package com.processing.terminalsimulator.util;

import org.springframework.stereotype.Component;

@Component
public class StanGenerator {
    private int stanCounter = 1;

    public String getNextStan() {
        int stan = stanCounter;
        stanCounter++;
        if (stanCounter > 999999) {
            stanCounter = 1;
        }
        return String.format("%06d", stan);
    }

}
