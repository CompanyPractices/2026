package com.processing.model;

import com.processing.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "mti", nullable = false, length = 4)
    private String mti;

    @Column(name = "stan", nullable = false, length = 6)
    private String stan;

    @Column(name = "rrn", length = 12)
    private String rrn;

    @Column(name = "pan", nullable = false, length = 16)
    private String pan;

    @Column(name = "processing_code", nullable = false, length = 6)
    private String processingCode;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "terminal_id", nullable = false, length = 8)
    private String terminalId;

    @Column(name = "merchant_id", nullable = false, length = 15)
    private String merchantId;

    @Column(name = "mcc", nullable = false, length = 4)
    private String mcc;

    @Column(name = "acquirer_id", nullable = false, length = 10)
    private String acquirerId;

    @Column(name = "issuer_id", length = 10)
    private String issuerId;

    @Column(name = "acquiring_fee")
    private Long acquiringFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "decline_reason", length = 100)
    private String declineReason;

    @Column(name = "auth_code", length = 6)
    private String authCode;

    @Column(name = "transmission_datetime", nullable = false)
    private Instant transmissionDateTime;

    @Column(name = "created_at")
    private Instant createdAt;
}
