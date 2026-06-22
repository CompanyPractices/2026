package com.processing.authorization.client;

import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.cardmanagement.CardModel;

import java.math.BigDecimal;

public interface CardManagementClient {
    CardModel getCard(String pan);

    void reserve(BigDecimal amount, String rrn, String pan);

    void rollback(RollbackRequest request);
}
