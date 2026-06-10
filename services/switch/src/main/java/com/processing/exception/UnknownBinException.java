package com.processing.exception;

public class UnknownBinException extends RuntimeException {

    public UnknownBinException(String bin) {
        super(String.format("Unknown BIN '%s'", bin));
    }
}
