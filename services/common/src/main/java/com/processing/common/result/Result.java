package com.processing.common.result;

/**
 * Represents the Result type pattern for operations that can either succeed with a value
 * or fail with an error.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * public Result<ApiKey, KeyError> getApiKey(String key) {
 *     ApiKey apiKey = repository.get(key);
 *
 *     if (apiKey == null) {
 *         return Result.failure(new KeyError.NotFound());
 *     }
 *
 *     return Result.success(apiKey);
 * }
 *
 * public Response validateKey(String key) {
 *     Result<ApiKey, KeyError> result = getApiKey(key);
 *
 *     return switch (result) {
 *         case Result.Success(ApiKey value) -> new Response("valid", value);
 *         case Result.Failure(KeyError.NotFound error) -> new Response("invalid", error.reason());
 *     };
 * }
 * }</pre>
 *
 * @param <T> the type of the value in case of a successful result
 * @param <E> the type of the error in case of a failed result
 */
public sealed interface Result<T, E> {
    record Success<T, E> (T value) implements Result<T, E> {}
    record Failure<T, E> (E error) implements Result<T, E> {}
}
