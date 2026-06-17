package com.processing.merchantacquirer.exceptions;

import com.processing.common.dto.ErrorResponse;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.merchantacquirer.exception.GlobalExceptionHandler;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalHandlerExceptionTest {
    private static final String SERVICE = "Merchant acquirer simulator";
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleNotValid() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "simulatorRequest");
        bindingResult.addError(new FieldError("simulatorRequest", "count", "must be grater than or equal to 1"));
        Method method = GlobalHandlerExceptionTest.class.getDeclaredMethod("validationTarget", Object.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotValid(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request", response.getBody().error());
        assertEquals(SERVICE, response.getBody().serviceName());
        assertEquals("0", response.getBody().retryAfterMs());
    }

    @Test
    void handleNotReadableReturnBadRequests() {
        HttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("bad body", null, inputMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotReadable(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request", response.getBody().error());
        assertEquals(SERVICE, response.getBody().serviceName());
        assertEquals("0", response.getBody().retryAfterMs());
    }

    @Test
    void handleIllegalArgumemnt() {
        IllegalArgumentException exception = new IllegalArgumentException("bad arg");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request", response.getBody().error());
        assertEquals(SERVICE, response.getBody().serviceName());
        assertEquals("0", response.getBody().retryAfterMs());
    }

    @Test
    void handleNotFound() {
        ResourceNotFoundException exception = new ResourceNotFoundException("bad arg");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().error());
        assertEquals(SERVICE, response.getBody().serviceName());
        assertEquals("0", response.getBody().retryAfterMs());
    }

    @Test
    void handleExternalServiceExceptions() {
        ExternalServiceException exception = new ExternalServiceException("Gateway", "down", "1000");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleExternalService(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("External service error", response.getBody().error());
        assertEquals("Gateway", response.getBody().serviceName());
        assertEquals("down", response.getBody().message());
        assertEquals("1000", response.getBody().retryAfterMs());
    }

    @Test
    void handleExceptions() {
        RuntimeException exception = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal service error", response.getBody().error());
        assertEquals(SERVICE, response.getBody().serviceName());
        assertEquals("0", response.getBody().retryAfterMs());
    }

    private void validationTarget(Object ignoredObject) {}
}
