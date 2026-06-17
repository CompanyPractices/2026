package com.processing.cardmanagement.exceptions;

public final class RrnAlreadyExistsException extends CardManagementException {

    public RrnAlreadyExistsException(String rrn) {
        super("RRN number \"" + rrn + "\" already exists in the database");
    }
}
