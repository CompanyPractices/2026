package com.processing.merchantacquirer.controller;

import com.processing.merchantacquirer.controller.dto.SimulatorRequest;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SimulationController {
  private final SimulationService simulationService;

  @PostMapping("/api/simulator/merchant/run")
  public ResponseEntity<SimulatorResponse> run(@RequestBody @Valid SimulatorRequest request) {
    SimulatorResponse response = simulationService.run(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api/simulator/merchants")
  public ResponseEntity<List<Merchant>> merchants() {
    return ResponseEntity.ok(simulationService.getAllMerchants());
  }
}
