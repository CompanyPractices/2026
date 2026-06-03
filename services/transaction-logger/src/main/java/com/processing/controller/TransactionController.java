package com.processing.controller;

import com.processing.dto.TransactionSearchResponse;
import com.processing.service.TransactionService;
import com.processing.specification.TransactionFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionSearchResponse search(@Valid @ModelAttribute TransactionFilter filter) {
        return transactionService.search(filter);
    }
}
