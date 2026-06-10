package com.processing.terminalsimulator.strategy;

import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.model.TransactionType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class HighValueStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.HIGH_VALUE;
    }
    private final Random random = new Random();
    @Override
    public long calculateAmount(Card card) {
        return 10_000_000 + (long) (random.nextDouble() * 40_000_000);
    }
    @Override
    public String getMcc() {
        return new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
    }
    @Override
    public boolean isInvalidPan() {
        return false;
    }
}
