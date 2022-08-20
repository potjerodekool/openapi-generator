package io.github.potjerodekool.openapi.log;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A logger interface to allow logging to the log of the buildsystems.
 */
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

    void log(LogLevel level, @Nullable String message, @Nullable Throwable exception);

}
