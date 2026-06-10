package com.processing.cardmanagement.exceptions;

public final class CardNotFoundException extends CardManagementException {

    public CardNotFoundException() {
        super("Card with present PAN was not found");
    }
}
