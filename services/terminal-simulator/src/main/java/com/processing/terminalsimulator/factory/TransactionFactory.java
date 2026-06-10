package com.processing.terminalsimulator.factory;

import com.processing.terminalsimulator.dto.AuthorizationRequest;
import com.processing.terminalsimulator.dto.Card;
import com.processing.terminalsimulator.model.CardStatus;
import com.processing.terminalsimulator.model.TerminalType;
import com.processing.terminalsimulator.model.TransactionType;
import com.processing.terminalsimulator.strategy.TransactionStrategy;
import com.processing.terminalsimulator.util.DateTimeGenerator;
import com.processing.terminalsimulator.util.StanGenerator;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TransactionFactory {
    private final Map<TransactionType, TransactionStrategy> strategies;
    private final DateTimeGenerator dateTimeGenerator;
    private final StanGenerator stanGenerator;

    public TransactionFactory(List<TransactionStrategy> strategyList, StanGenerator stanGenerator,
                              DateTimeGenerator dateTimeGenerator) {
        this.stanGenerator = stanGenerator;
        this.dateTimeGenerator = dateTimeGenerator;

        this.strategies = new EnumMap<>(TransactionType.class);
        for (TransactionStrategy strategy : strategyList) {
            this.strategies.put(strategy.getType(), strategy);
        }
    }

    public AuthorizationRequest create(TransactionType transactionType, String partOfDay, Card card) {
        TransactionStrategy transactionStrategy = strategies.get(transactionType);
        long amount = transactionStrategy.calculateAmount(card);
        String mcc = transactionStrategy.getMcc();
        String pan = transactionStrategy.isInvalidPan() ? getInvalidPan(card) : card.pan();
        String terminalId = String.format("TERM%04d", ThreadLocalRandom.current().nextInt(1, 10_000));
        String terminalType = String.valueOf(TerminalType.values()[(int) (Math.random() * 3)]);
        String merchantId = String.format("MERCH%10d", ThreadLocalRandom.current().nextLong(1, 10_000_000_000L));
        String acquirerId = String.format("ACQ%03d", ThreadLocalRandom.current().nextLong(1, 1000));

        return AuthorizationRequest.builder()
                .mti("0100")
                .stan(stanGenerator.getNextStan())
                .pan(pan)
                .processingCode("000000")
                .amount(amount)
                .currencyCode(card.currencyCode())
                .transmissionDateTime(dateTimeGenerator.generate(partOfDay))
                .terminalId(terminalId)
                .terminalType(terminalType)
                .merchantId(merchantId)
                .mcc(mcc)
                .acquirerId(acquirerId)
                .issuerId("")
                .build();
    }

    public CardStatus getRequiredStatus(TransactionType type) {
        return strategies.get(type).getRequiredCardStatus();
    }

    private String getInvalidPan(Card card) {
        String validPan = card.pan();
        char last = validPan.charAt(validPan.length() - 1);
        char newLast = (last == '0') ? '1' : '0';
        return validPan.substring(0, validPan.length() - 1) + newLast;
    }
}
