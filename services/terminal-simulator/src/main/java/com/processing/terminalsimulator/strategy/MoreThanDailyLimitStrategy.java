package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MoreThanDailyLimitStrategy implements TransactionStrategy {
    private final Random random = new Random();

    @Override
    public TransactionType getType() {
        return TransactionType.MORE_THAN_DAILY_LIMIT;
    }
    @Override
    public long calculateAmount(CardModel card) {
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
