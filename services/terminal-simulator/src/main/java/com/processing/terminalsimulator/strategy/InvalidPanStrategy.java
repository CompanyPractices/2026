package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class InvalidPanStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.INVALID_PAN;
    }
    @Override
    public long calculateAmount(CardModel card) {
        return 10_000 + (long) (Math.random() * 490_000);
    }
    @Override
    public String getMcc() {
        return new String[]{"5411", "5812", "5814", "5732", "5399", "4814",
                "7994", "3501"}[ThreadLocalRandom.current().nextInt(8)];
    }
    @Override
    public boolean isInvalidPan() {
        return true;
    }
}
