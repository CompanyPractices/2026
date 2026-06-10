package com.processing.authorization.exceptions;

/**
 * Исключение, которое используется при невозможночти подключения к другому
 * сервису.
 *
 * @see Exception
 */
public class ServiceUnavailableException extends Exception {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
