package com.processing.terminalsimulator.strategy;

import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class AlmostDailyLimitStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.ALMOST_DAILY_LIMIT;
    }
    @Override
    public long calculateAmount(Card card) {
        return  card.dailyLimit() - 1;
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
