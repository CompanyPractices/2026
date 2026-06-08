package com.processing.merchantacquirer.controller;

import com.processing.merchantacquirer.controller.dto.HealthResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("ok", "merchant-acquirer-simulator", Map.of()));
  }
}
