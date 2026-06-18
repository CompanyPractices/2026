package com.processing.merchantacquirer.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class StanGenerator {
  private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

  public String next(String terminalId) {
    if (!counters.containsKey(terminalId)) {
      counters.put(terminalId, new AtomicInteger(0));
    }

    int value = counters.get(terminalId).updateAndGet(i -> i >= 999999 ? 1 : i + 1);
    return String.format("%06d", value);
  }
}
