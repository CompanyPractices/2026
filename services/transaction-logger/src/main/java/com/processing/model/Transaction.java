package com.processing.model;

import com.processing.common.dto.transactionlogger.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at"),
        @Index(name = "idx_transactions_pan", columnList = "pan"),
        @Index(name = "idx_transactions_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class Transaction {
    @Id
    @Column(nullable = false)
    private UUID id;
    @Column(length = 4)
    private String mti;
    @Column(nullable = false, length = 6)
    private String stan;
    @Column(length = 12)
    private String rrn;
    @Column(nullable = false, length = 16)
    private String pan;
    @Column(nullable = false, length = 6)
    private String processingCode;
    @Column(nullable = false)
    private Long amount;
    @Column(nullable = false, length = 3)
    private String currencyCode;
    @Column(nullable = false, length = 8)
    private String terminalId;
    @Column(nullable = false, length = 15)
    private String merchantId;
    @Column(nullable = false, length = 4)
    private String mcc;
    @Column(nullable = false, length = 10)
    private String acquirerId;
    @Column(length = 10)
    private String issuerId;
    private Long acquiringFee;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;
    @Column(length = 100)
    private String declineReason;
    @Column(length = 6)
    private String authCode;
    private Integer processingTimeMs;
    @Column(nullable = false)
    private Instant transmissionDateTime;
    private Instant createdAt;
}
