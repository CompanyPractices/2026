package com.processing.controller;

import com.processing.dto.TransactionRequest;
import com.processing.service.TransactionStoreResult;
import com.processing.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalTransactionController {
    private final TransactionService transactionService;

    @PostMapping("/log")
    public ResponseEntity<?> store(@Valid @RequestBody TransactionRequest request) {
        TransactionStoreResult result = transactionService.store(request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.storedTransaction());
        }

        return ResponseEntity.ok(result.existingTransaction());
    }
}
