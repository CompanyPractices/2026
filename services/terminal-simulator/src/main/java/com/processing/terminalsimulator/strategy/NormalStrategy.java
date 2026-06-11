package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class NormalStrategy implements TransactionStrategy {
    private final Random random = new Random();

    @Override
    public TransactionType getType() {
        return TransactionType.NORMAL;
    }
    @Override
    public long calculateAmount(CardModel card) {
        return 10_000 + (long) (random.nextDouble() * 490_000);
    }
    @Override
    public String getMcc() {
        return "5411";
    }
    @Override
    public boolean isInvalidPan() {
        return false;
    }
}
