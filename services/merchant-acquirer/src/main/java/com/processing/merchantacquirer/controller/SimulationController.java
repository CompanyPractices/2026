package com.processing.merchantacquirer.controller;

import com.processing.merchantacquirer.controller.dto.SimulatorRequest;
import com.processing.merchantacquirer.controller.dto.SimulatorResponse;
import com.processing.merchantacquirer.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SimulationController {
    private final SimulationService simulationService;

    @PostMapping("/api/simulator/merchant/run")
    public ResponseEntity<SimulatorResponse> run(@RequestBody SimulatorRequest request) {
        SimulatorResponse response = simulationService.run(request);
        return ResponseEntity.ok(response);
    }
}
