package com.processing.controller;

import com.processing.dto.HealthResponse;
import com.processing.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final TransactionRepository transactionRepository;

    public HealthController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "transaction-logger",
                transactionRepository.count()
        ));
    }
}
