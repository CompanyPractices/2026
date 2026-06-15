package com.processing.authorization.exceptions;

/**
 * Исключение, которое используется при ненахождении карты в CMS
 *
 * @see Exception
 */
public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }
}
