package com.processing.cardmanagement.exceptions;

public final class RollbackAlreadySatisfied extends CardManagementException {

    public RollbackAlreadySatisfied(String rrn) {
        super("Rollback with RRN \"" + rrn + "\" is already satisfied");
    }
}
