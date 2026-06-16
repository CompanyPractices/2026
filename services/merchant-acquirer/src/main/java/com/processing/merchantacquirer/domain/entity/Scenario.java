package com.processing.merchantacquirer.domain.entity;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Scenario {
  private List<String> mcc;
  private BigDecimal countLower;
  private BigDecimal countUpper;
  private String timeLower;
  private String timeUpper;
  private int avgApproved;
}
