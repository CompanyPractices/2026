package com.processing.cardmanagement.exceptions;

/**
 * Выбрасывается, когда карта с указанным PAN не найдена в базе данных
 */
public final class CardNotFoundException extends CardManagementException {

    public CardNotFoundException() {
        super("Card with present PAN was not found");
    }
}
