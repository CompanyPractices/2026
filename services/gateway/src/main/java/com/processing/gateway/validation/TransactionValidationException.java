package com.processing.gateway.validation;

public class TransactionValidationException extends RuntimeException {
  public TransactionValidationException(String message) {
    super(message);
  }
}
