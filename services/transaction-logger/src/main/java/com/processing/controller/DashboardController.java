package com.processing.controller;

import com.processing.dto.DashboardStatsResponse;
import com.processing.model.Transaction;
import com.processing.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TransactionService transactionService;

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return transactionService.getStats();
    }

    @GetMapping("/recent")
    public List<Transaction> getRecent(@RequestParam(defaultValue = "20") int limit) {
        return transactionService.getRecent(limit);
    }
}
