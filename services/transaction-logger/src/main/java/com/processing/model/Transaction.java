package com.processing.model;

import com.processing.enums.TransactionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    private UUID id;
    private String mti;
    private String stan;
    private String rrn;
    private String pan;
    private String processingCode;
    private Long amount;
    private String currencyCode;
    private String terminalId;
    private String merchantId;
    private String mcc;
    private String acquirerId;
    private String issuerId;
    private Long acquiringFee;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private String declineReason;
    private String authCode;
    private Instant transmissionDateTime;
    private Instant createdAt;
}
