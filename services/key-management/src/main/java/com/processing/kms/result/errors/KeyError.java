package com.processing.kms.result.errors;

public sealed interface KeyError {
    record KeyExpiredError() implements KeyError {}
    record KeyNotFoundError() implements KeyError {}
}
