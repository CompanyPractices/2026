package com.processing.cardmanagement.exceptions;

/**
 * Базовое исключение для всех исключений сервиса управления картами
 */
public abstract class CardManagementException extends RuntimeException {

    public CardManagementException(String message) {
        super(message);
    }
}
