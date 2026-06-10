package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcquirerFee {
    @Id
    private String transmissionDateTime;
    private String stan;
    private String terminalId;
    private Long acquirerFee;
}
