package com.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Logger {

    static Logger getLogger(final String name) {
        return LoggerFactory.getLogger(name);
    }

    default void info(final String message) {
        log(LogLevel.INFO, message);
    }

    default void log(final LogLevel level, final @Nullable String message) {
        this.log(level, message, null);
    }

    default void log(final LogLevel level, final @Nullable Throwable exception) {
        this.log(level, null, exception);
    }

    void log(LogLevel level, @Nullable String message, @Nullable Throwable exception);

}
