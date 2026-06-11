package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.terminalsimulator.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class NormalStrategy implements TransactionStrategy {
    @Override
    public TransactionType getType() {
        return TransactionType.NORMAL;
    }
    @Override
    public long calculateAmount(CardModel card) {
        double randomDouble = ThreadLocalRandom.current().nextDouble();
        return 10_000 + (long) (randomDouble * 490_000);
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
