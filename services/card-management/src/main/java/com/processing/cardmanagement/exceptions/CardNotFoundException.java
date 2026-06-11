package com.processing.cardmanagement.exceptions;

public final class CardNotFoundException extends CardManagementException {

    public CardNotFoundException(String maskedPan) {
        super("Card with PAN " + maskedPan + " was not found");
    }
}
