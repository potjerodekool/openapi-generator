package io.github.potjerodekool.openapi.internal.di;

public class DIException extends RuntimeException {

    public DIException(final String message) {
        super(message);
    }

    public DIException(final String message,
                       final Throwable cause) {
        super(message, cause);
    }
}
