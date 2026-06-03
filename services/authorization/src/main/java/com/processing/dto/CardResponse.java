package com.processing.dto;

import com.processing.enums.CardStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class CardResponse {
    private UUID id;

    private String pun;

    private String bin;

    private String cardholderName;

    private String expiryDate;

    private CardStatus status;

    private String currencyCode;

    private Integer dailyLimit;

    private Integer monthlyLimit;

    private Integer availableBalance;

    private String issuerId;

    private LocalDate createdAt;
}
