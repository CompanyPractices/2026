package com.processing.exceptions;

public class CardNotFoundException extends CardManagementException {

    public CardNotFoundException() {
        super("Card with present PAN was not found");
    }
}
