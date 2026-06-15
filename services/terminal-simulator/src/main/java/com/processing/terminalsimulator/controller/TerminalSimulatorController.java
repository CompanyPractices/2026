package com.processing.terminalsimulator.controller;

import com.processing.common.dto.terminalsimulator.TerminalRunRequest;
import com.processing.common.dto.terminalsimulator.TerminalRunResponse;
import com.processing.terminalsimulator.service.TerminalSimulatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulator/terminal")
@RequiredArgsConstructor
public class TerminalSimulatorController {

    private final TerminalSimulatorService simulatorService;

    @PostMapping("/run")
    public ResponseEntity<TerminalRunResponse> run(@Valid @RequestBody TerminalRunRequest request) {
        TerminalRunResponse response = simulatorService.run(request.count(), request.scenario());
        return ResponseEntity.ok(response);
    }
}
