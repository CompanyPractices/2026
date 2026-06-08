package com.processing.authorization.dto;

import com.processing.authorization.enums.CardStatus;
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

    private LocalDate expiryDate;

    private CardStatus status;

    private String currencyCode;

    private Long dailyLimit;

    private Long monthlyLimit;

    private Long availableBalance;

    private String issuerId;

    private LocalDate createdAt;
}
