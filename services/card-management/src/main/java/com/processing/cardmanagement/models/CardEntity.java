package com.processing.cardmanagement.models;

import com.processing.common.dto.cardmanagement.CardStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "cards", indexes = {
    @Index(name = "uk_cards_pan", columnList = "pan", unique = true),
    @Index(name = "idx_cards_issuer_id_created_at", columnList = "issuer_id, created_at"),
    @Index(name = "idx_cards_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE users SET status = 'DELETED' WHERE id = ?")
@SQLRestriction("status <> 'DELETED'")
public class CardEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(length = 16, unique = true, nullable = false)
    private String pan;

    @Column(length = 6, nullable = false)
    private String bin;

    @Column(nullable = false)
    private String cardholderName;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, length = 4)
    private String expiryDate;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(length = 3, nullable = false)
    private String currencyCode;

    private long dailyLimit;

    private long monthlyLimit;

    private long availableBalance;

    @Column(length = 10, nullable = false)
    private String issuerId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
