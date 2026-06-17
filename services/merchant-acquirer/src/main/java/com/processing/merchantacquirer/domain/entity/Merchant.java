package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Merchants")
public class Merchant {
  @Id private String id;
  private String name;
  private String mcc;
  private String category;
  private String acquirerId;
  @Column(name = "acquiring_fee", precision = 5)
  private BigDecimal acquiringFee;
  private BigDecimal averageCheck;
}
