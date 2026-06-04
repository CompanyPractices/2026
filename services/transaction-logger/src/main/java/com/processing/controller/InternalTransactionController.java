package com.processing.controller;

import com.processing.dto.TransactionRequest;
import com.processing.dto.TransactionResponse;
import com.processing.dto.TransactionStoredResponse;
import com.processing.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalTransactionController {
    private final TransactionService transactionService;

    @PostMapping("/log")
    public ResponseEntity<?> store(@Valid @RequestBody TransactionRequest request) {
        Optional<TransactionResponse> existingTransaction = transactionService.findExistingTransaction(request.id());
        if (existingTransaction.isPresent()) {
            return ResponseEntity.ok(existingTransaction.get());
        }

        TransactionStoredResponse response = transactionService.store(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
