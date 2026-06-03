package com.processing.controller;

import com.processing.dto.DashboardStatsResponse;
import com.processing.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TransactionService transactionService;

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return transactionService.getStats();
    }
}
