package com.processing.cardmanagement.exceptions;

public final class RrnNotFoundException extends CardManagementException {

    public RrnNotFoundException(String rrn) {
        super("Can not find reservation with RRN \"" + rrn + "\"");
    }
}
