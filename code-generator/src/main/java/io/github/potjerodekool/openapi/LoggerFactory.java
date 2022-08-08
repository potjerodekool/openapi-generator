package io.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 A logger factory to create Logger instances.
 Build systems should set the logger provider like this:
 LoggerFactory#setLoggerProvider(mavenLoggerProvider)
 */
public class LoggerFactory {

    private static final LoggerFactory FACTORY = new LoggerFactory();

    private Function<String, Logger> loggerProvider = LoggerFactory::devNullLoggerProvider;

    private LoggerFactory() {
    }

    public static void setLoggerProvider(final Function<String, Logger> loggerProvider) {
        FACTORY.loggerProvider = loggerProvider;
    }

    public static Logger getLogger(final String name) {
        return FACTORY.loggerProvider.apply(name);
    }

    private static Logger devNullLoggerProvider(final String name) {
        return LoggerFactory::devNullLogger;
    }

    @SuppressWarnings("EmptyMethod")
    private static void devNullLogger(final LogLevel level,
                                      final @Nullable String message,
                                      final @Nullable Throwable exception) {
        //Nothing to do here
    }
}
