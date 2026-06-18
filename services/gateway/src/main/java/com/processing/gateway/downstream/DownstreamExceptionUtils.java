package com.processing.gateway.downstream;

import jakarta.servlet.ServletException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

public final class DownstreamExceptionUtils {

    private DownstreamExceptionUtils() {
    }

    public static boolean isDownstreamUnavailable(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ResourceAccessException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof HttpConnectTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    public static void rethrow(Exception exception) throws ServletException, IOException {
        if (exception instanceof ServletException servletException) {
            throw servletException;
        }
        if (exception instanceof IOException ioException) {
            throw ioException;
        }
        if (exception instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        throw new ServletException(exception);
    }
}
