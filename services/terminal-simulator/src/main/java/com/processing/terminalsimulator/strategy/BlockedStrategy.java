package com.processing.terminalsimulator.strategy;

import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.model.CardStatus;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class BlockedStrategy implements TransactionStrategy {
    private final Random random = new Random();

    @Override
    public TransactionType getType() {
        return TransactionType.BLOCKED;
    }
    @Override
    public long calculateAmount(Card card) {
        return 10_000 + (long) (random.nextDouble() * 490_000);
    }
    @Override
    public String getMcc() {
        return new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
    }
    @Override
    public CardStatus getRequiredCardStatus() {
        return CardStatus.BLOCKED;
    }
    @Override
    public boolean isInvalidPan() {
        return false;
    }
}
