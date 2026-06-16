package com.processing.cardmanagement.exceptions;

public final class RrnNotFound extends CardManagementException {

    public RrnNotFound(String rrn) {
        super("Can not find reservation with RRN \"" + rrn + "\"");
    }
}
