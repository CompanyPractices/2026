package com.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RunResponse {
    private int totalSubmitted;
    private int approved;
    private int declined;
    private long elapsedMs;
    private List<AuthorizationResponse> transactions;
}
