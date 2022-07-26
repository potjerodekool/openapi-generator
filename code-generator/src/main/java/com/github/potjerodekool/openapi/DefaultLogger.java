package com.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DefaultLogger implements Logger {

    private final String name;

    public DefaultLogger(final String name) {
        this.name = name;
    }

    @Override
    public void log(final LogLevel level, final @Nullable String message, final @Nullable Throwable exception) {
        final var outputStream = level == LogLevel.INFO || level == LogLevel.WARNING ? System.out : System.err;
        final var fullMessage = name + " " + level.name() + " " + (message != null ? message : "");

        outputStream.print(fullMessage);

        if (exception != null) {
            final var stringWriter = new StringWriter();
            final var printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            printWriter.flush();
            outputStream.print(stringWriter);
        }
    }

}
