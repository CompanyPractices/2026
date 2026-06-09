package com.processing.merchantacquirer.domain;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class StanGenerator {
  private final AtomicInteger counter = new AtomicInteger(0);

  public String next() {
    int value = counter.addAndGet(1);
    if (value > 999999) {
      counter.set(1);
      value = counter.get();
    }
    return String.format("%06d", value);
  }
}
