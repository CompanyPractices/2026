package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


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
    private BigDecimal acquirerFee;
    private BigDecimal amount;
}
