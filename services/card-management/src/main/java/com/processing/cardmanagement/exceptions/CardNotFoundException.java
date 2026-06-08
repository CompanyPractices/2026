package com.processing.cardmanagement.exceptions;

/**
 * Выбрасывается, когда карта с указанным PAN не найдена в базе данных
 */
public final class CardNotFoundException extends CardManagementException {

    public CardNotFoundException(String maskedPan) {
        super("Card with PAN " + maskedPan + " was not found");
    }
}
