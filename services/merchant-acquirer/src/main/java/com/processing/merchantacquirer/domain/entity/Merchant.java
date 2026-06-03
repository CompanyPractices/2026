package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Merchant {
    @Id
    private String id;
    private String name;
    private String mcc;
    private String category;
    private String acquirerId;
    private int acquiringFee;
    private Long averageCheck;
}
