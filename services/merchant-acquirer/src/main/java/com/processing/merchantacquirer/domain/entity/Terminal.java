package com.processing.merchantacquirer.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Terminal {
  private String id;
  private String type;
}
