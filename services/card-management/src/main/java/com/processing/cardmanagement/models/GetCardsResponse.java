package com.processing.cardmanagement.models;

import java.util.List;

public record GetCardsResponse(int total, List<CardDto> cards) {
}
