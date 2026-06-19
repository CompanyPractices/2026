package com.processing.cardmanagement.exceptions;

public final class BinAlreadyExistException extends RuntimeException {

    public BinAlreadyExistException(String bin) {
        super("BIN already exist: " + bin);
    }
}
