package io.github.potjerodekool.openapi.internal;

public class GenerateException extends RuntimeException {

    public GenerateException(final String message) {
        super(message);
    }

    public GenerateException(final Throwable cause) {
        super(cause);
    }
}
