package com.processing.cardmanagement.exceptions;

public final class RrnAlreadyExists extends CardManagementException {

    public RrnAlreadyExists(String rrn) {
        super("RRN number \"" + rrn + "\" already exists in the database");
    }
}
