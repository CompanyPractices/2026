package com.processing.models;

import java.util.List;

public record GenerateCardResponse(int generated, List<CardDto> cards) {}
