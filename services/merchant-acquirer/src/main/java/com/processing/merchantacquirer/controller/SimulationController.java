package com.processing.merchantacquirer.controller;

import com.processing.merchantacquirer.dto.AuthorizationResponse;
import com.processing.merchantacquirer.dto.SimulatorRequest;
import com.processing.merchantacquirer.dto.SimulatorResponse;
import com.processing.merchantacquirer.dto.TransactionResponse;
import com.processing.merchantacquirer.service.SimulationService;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@RestController
public class SimulationController {
    private final SimulationService simulationService = new SimulationService();

    @PostMapping("/api/simulator/merchant/run")
    public ResponseEntity<SimulatorResponse> run(@RequestBody SimulatorRequest request) {
        SimulatorResponse response = simulationService.run(request);
        return ResponseEntity.ok(response);
    }
}
