package com.processing.authorization.exceptions;

/**
 * Исключение, которое используется при невозможности зарезервировать средства
 * на карте в CMS
 *
 * @see Exception
 */
public class ReserveException extends RuntimeException {
    public ReserveException(String message) {
        super(message);
    }
}
