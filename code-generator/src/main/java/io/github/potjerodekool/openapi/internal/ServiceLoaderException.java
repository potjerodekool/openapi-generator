package io.github.potjerodekool.openapi.internal;

import org.checkerframework.checker.nullness.qual.Nullable;

public class ServiceLoaderException extends RuntimeException {

    public ServiceLoaderException(final String message) {
        this(message, null);
    }

    public ServiceLoaderException(final String message,
                                  final @Nullable Throwable cause) {
        super(message, cause);
    }
}
