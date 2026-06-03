package com.processing.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class GenerateCardsRequest {
    @Min(1)
    private int count;

    @NotEmpty
    private List<String> bins;
}
