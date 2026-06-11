package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class NoMoneyStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.NO_MONEY;
    }
    @Override
    public long calculateAmount(CardModel card) {
        double randomDouble = ThreadLocalRandom.current().nextDouble();
        return card.availableBalance() + (long) (randomDouble * 100_000);
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
