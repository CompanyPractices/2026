package com.processing.cardmanagement.exceptions;

/**
 * Выбрасывается, когда доступный баланс карты недостаточен для резервирования
 */
public final class InsufficientFundsException extends CardManagementException {

    public InsufficientFundsException() {
        super("Not enough funds on this account");
    }
}
