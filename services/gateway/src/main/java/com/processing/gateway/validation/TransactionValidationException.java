package com.processing.gateway.validation;

/**
 * Signals that a transaction authorization request failed gateway validation
 */
public class TransactionValidationException extends RuntimeException {
  /**
   * Creates a validation exception with a client-facing message
   *
   * @param message validation error description
   */
  public TransactionValidationException(String message) {
    super(message);
  }
}
