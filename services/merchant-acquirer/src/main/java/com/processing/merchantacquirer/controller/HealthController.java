package com.processing.merchantacquirer.controller;

import com.processing.merchantacquirer.controller.dto.HealthResponse;

import com.processing.merchantacquirer.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {
  private final SimulationService simulationService;

  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("ok", "merchant-acquirer-simulator", simulationService.countMerchants()));
  }
}
