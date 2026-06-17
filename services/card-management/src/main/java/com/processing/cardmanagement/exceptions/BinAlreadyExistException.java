package com.processing.cardmanagement.exceptions;

public class BinAlreadyExistException extends RuntimeException {
    public BinAlreadyExistException(String bin) {
        super("BIN already exist: " + bin);
    }
}
