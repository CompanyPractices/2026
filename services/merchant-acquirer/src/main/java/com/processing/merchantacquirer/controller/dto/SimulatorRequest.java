package com.processing.merchantacquirer.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SimulatorRequest {
    private int count;

    @NotBlank
    private String scenario;

    private List<String> mccCodes;
}
