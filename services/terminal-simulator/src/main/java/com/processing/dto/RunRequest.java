package com.processing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RunRequest {
    @NotNull
    @Min(1)
    private Integer count;
    private String scenario = "normal";
}
