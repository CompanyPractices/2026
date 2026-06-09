package com.processing.common.dto.transaction;


import java.util.UUID;


public record LogResponse(
        UUID id,
        String status
) {}
