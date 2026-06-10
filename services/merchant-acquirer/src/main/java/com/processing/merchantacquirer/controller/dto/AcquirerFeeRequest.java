package com.processing.merchantacquirer.controller.dto;

public record AcquirerFeeRequest(
        String stan,
        String pan,
        String terminalID
){
}
