package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Merchants")
public class Merchant {
  @Id private String id;
  private String name;
  @Column(name = "mcc", length = 4)
  @JdbcTypeCode(Types.CHAR)
  private String mcc;
  private String category;
  private String acquirerId;
  @Column(name = "acquiring_fee", precision = 19, scale = 4)
  private BigDecimal acquiringFee;
  private BigDecimal averageCheck;
}
