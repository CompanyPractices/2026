package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import java.math.BigDecimal;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcquirerFee {
    @Id
    private Instant transmissionDateTime;
    private String stan;
    private String pan;
    private String terminalId;
    private BigDecimal acquirerFee;
    private BigDecimal amount;
}
