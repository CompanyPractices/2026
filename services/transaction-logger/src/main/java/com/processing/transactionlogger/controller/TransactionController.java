package com.processing.transactionlogger.controller;

import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.service.TransactionService;
import com.processing.transactionlogger.specification.TransactionFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transactions", description = "Поиск транзакций")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @Operation(
            summary = "Поиск транзакций",
            description = "Фильтр с пагинацией. Все параметры опциональны, активные объединяются через AND.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Результаты поиска"),
                    @ApiResponse(responseCode = "400", description = "Невалидные параметры",
                    content = @Content(schema = @Schema(example = "{\"limit\": "
                            + "\"must be greater than 0\"")))
            }
    )
    @GetMapping("/search")
    public TransactionSearchResponse search(@Valid @ModelAttribute TransactionFilter filter) {
        return transactionService.search(filter);
    }
}
