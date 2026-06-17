package com.processing.kms.errors;

public sealed interface KeyError {
    record Expired() implements KeyError {
        public String message() { return "Key expired"; }
    }
    record NotFound() implements KeyError  {
        public String message() { return "Key not found"; }
    }
    record OwnerIdMismatch() implements KeyError  {
        public String message() { return "Owner ID mismatch"; }
    }
}
