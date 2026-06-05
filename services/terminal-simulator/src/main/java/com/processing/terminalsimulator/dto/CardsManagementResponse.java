package com.processing.terminalsimulator.dto;

import java.util.List;

public record CardsManagementResponse(
        int total,
        List<Card> cards
) {}
