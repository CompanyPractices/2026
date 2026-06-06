package com.processing.merchantacquirer.client.dto;

import java.util.List;

public record CardsResponse(
        Integer total,
        List<CardDataResponse> cards
) {}
