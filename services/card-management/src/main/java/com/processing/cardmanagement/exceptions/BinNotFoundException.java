package com.processing.cardmanagement.exceptions;

public class BinNotFoundException extends CardManagementException {
    public BinNotFoundException(String bin) {
        super("BIN not found: " + bin);
    }
}
