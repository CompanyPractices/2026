package com.processing.terminalsimulator.strategy;

import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.model.CardStatus;
import com.processing.terminalsimulator.model.TransactionType;

public interface TransactionStrategy {
    TransactionType getType();
    long calculateAmount(Card card);
    String getMcc();
    boolean isInvalidPan();
    default CardStatus getRequiredCardStatus() {
        return CardStatus.ACTIVE;
    }
}
