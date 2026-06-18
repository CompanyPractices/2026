package com.processing.merchantacquirer.domain.entity;

import com.processing.common.dto.authorization.AuthorizationRequest;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import java.math.BigDecimal;


@Entity
@Data
@NoArgsConstructor
public class AcquirerFee {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acquirer_fee_seq")
    @SequenceGenerator(name = "acquirer_fee_seq", sequenceName = "acquirer_fee_seq", allocationSize = 100)
    private Long id;

    @Column(name = "transmission_date_time", nullable = false)
    private Instant transmissionDateTime;
    private String stan;
    private String pan;
    @Column(name = "terminal_id")
    private String terminalId;
    private BigDecimal acquirerFee;
    private BigDecimal amount;

    public AcquirerFee(
            String transmissionDateTime, String stan, String pan, String terminalId, BigDecimal acquirerFee, BigDecimal amount) {
        this.transmissionDateTime = transmissionDateTime;
        this.stan = stan;
        this.pan = pan;
        this.terminalId = terminalId;
        this.acquirerFee = acquirerFee;
        this.amount = amount;
    }

    public static AcquirerFee of(BigDecimal fee, AuthorizationRequest request) {
        return new AcquirerFee(
                request.transmissionDateTime(),
                request.stan(),
                request.pan(),
                request.terminalId(),
                fee,
                request.amount()
        );
    }
}
