package com.processing.dto;

import java.util.UUID;

public record TransactionStoredResponse(
        UUID id,
        String status
) {
}
