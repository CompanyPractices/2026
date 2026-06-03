package com.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReserveRequest {
    private Integer amount;

    private String rrn;
}
