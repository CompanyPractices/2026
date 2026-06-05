package com.processing.controller;

import com.processing.dto.DashboardStatsResponse;
import com.processing.model.Transaction;
import com.processing.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@Tag(name = "Dashboard", description = "Статистика и последние транзакции")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TransactionService transactionService;

    @Operation(summary = "Агрегированная статистика", responses = {
            @ApiResponse(responseCode = "200", description = "Статистика по всем транзакциям")
    })
    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return transactionService.getStats();
    }

    @Operation(summary = "Последние транзакции", responses = {
            @ApiResponse(responseCode = "200", description = "Список транзакций, отсортированных по createdAt DESC")
    })
    @GetMapping("/recent")
    public List<Transaction> getRecent(@Positive(message = "limit must be positive")
                                           @Max(value = 500, message = "limit must not exceed 500")
                                           @RequestParam(defaultValue = "20") int limit) {
        return transactionService.getRecent(limit);
    }
}
