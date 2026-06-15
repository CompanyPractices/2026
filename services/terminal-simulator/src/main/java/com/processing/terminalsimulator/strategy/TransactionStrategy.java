package com.processing.terminalsimulator.strategy;

import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.common.dto.cardmanagement.CardModelStatus;
import com.processing.terminalsimulator.model.TransactionType;

public interface TransactionStrategy {
    TransactionType getType();
    long calculateAmount(CardModel card);
    String getMcc();
    boolean isInvalidPan();
    default CardModelStatus getRequiredCardStatus() {
        return CardModelStatus.ACTIVE;
    }
}
