package com.processing.merchantacquirer.domain.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Scenario {
  private List<String> mcc;
  private int countLower;
  private int countUpper;
  private String timeLower;
  private String timeUpper;
  private int avgApproved;
}
