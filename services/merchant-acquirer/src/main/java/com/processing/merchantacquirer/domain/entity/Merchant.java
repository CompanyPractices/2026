package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private Double acquiringFee;
  private Long averageCheck;
}
