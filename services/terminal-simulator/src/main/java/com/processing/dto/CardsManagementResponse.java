package com.processing.dto;

import java.util.List;

public record CardsManagementResponse(
        int total,
        List<Card> cards
) {}
