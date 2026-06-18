package com.processing.merchantacquirer.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Terminals")
public class Terminal {
  @Id
  private String id;
  private String type;
  @ManyToOne
  private Merchant merchant;
}
