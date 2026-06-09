package com.processing.transactionlogger.controller;

import com.processing.transactionlogger.dto.HealthResponse;
import com.processing.transactionlogger.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Состояние сервиса")
@RestController
public class HealthController {

    private final TransactionRepository transactionRepository;

    public HealthController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Operation(summary = "Health-check")
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "ok",
                "transaction-logger",
                transactionRepository.count()
        ));
    }
}
