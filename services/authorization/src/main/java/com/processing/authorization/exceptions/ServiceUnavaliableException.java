package com.processing.authorization.exceptions;

/**
 * Исключение, которое используется при невозможночти подключения к другому
 * сервису.
 *
 * @see Exception
 */
public class ServiceUnavaliableException extends Exception {
    public ServiceUnavaliableException(String message) {
        super(message);
    }
}
