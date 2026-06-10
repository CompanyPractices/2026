package com.processing.terminalsimulator.strategy;

import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.model.TransactionType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MoreThanDailyLimitStrategy implements TransactionStrategy {
    private final Random random = new Random();

    @Override
    public TransactionType getType() {
        return TransactionType.MORE_THAN_DAILY_LIMIT;
    }
    @Override
    public long calculateAmount(Card card) {
        return card.dailyLimit() + (long) (random.nextDouble() * 10_000);
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
