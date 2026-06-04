package com.processing.controller;

import com.processing.dto.RunRequest;
import com.processing.dto.RunResponse;
import com.processing.service.TerminalSimulatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/simulator/terminal")
@RequiredArgsConstructor
public class TerminalSimulatorController {

    private final TerminalSimulatorService simulatorService;

    @PostMapping("/run")
    public ResponseEntity<RunResponse> run(@Valid @RequestBody RunRequest request) {
        RunResponse response = simulatorService.run(request.count(), request.scenario());
        return ResponseEntity.ok(response);
    }
}
