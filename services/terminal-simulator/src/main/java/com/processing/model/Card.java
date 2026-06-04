package com.processing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Card {
    private long id;
    private String pan;
    private String bin;
    private String cardholderName;
    private String expiryDate;
    private CardStatus status;
    private String currencyCode;
    private int dailyLimit;
    private int monthlyLimit;
    private int availableBalance;
    private String issuerId;
    private String createdAt;
}
