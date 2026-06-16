package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
public class AcquirerFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transmission_date_time", nullable = false)
    private String transmissionDateTime;
    private String stan;
    private String pan;
    @Column(name = "terminal_id")
    private String terminalId;
    @Column(name = "acquirer_fee")
    private Long acquirerFee;
    private Long amount;

    public AcquirerFee(String transmissionDateTime, String stan, String pan, String terminalId, Long acquirerFee, Long amount) {
        this.transmissionDateTime = transmissionDateTime;
        this.stan = stan;
        this.pan = pan;
        this.terminalId = terminalId;
        this.acquirerFee = acquirerFee;
        this.amount = amount;
    }
}
