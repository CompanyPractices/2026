package com.processing.kms.result;

public sealed interface Result<T, E> {
    record Success<T, E> (T value) implements Result<T, E> {}
    record Failure<T, E> (E error) implements Result<T, E> {}
}
