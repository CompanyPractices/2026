package com.processing.authorization.exceptions;

/**
 * Исключение, которое используется при невозможности зарезервировать средства
 * на карте в CMS
 *
 * @see Exception
 */
public class ReserveCardException extends RuntimeException {
    public ReserveCardException(String message) {
        super(message);
    }
}
