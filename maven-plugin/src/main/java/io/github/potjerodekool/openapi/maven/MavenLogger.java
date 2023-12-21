package io.github.potjerodekool.openapi.maven;

import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import org.apache.maven.plugin.Mojo;

public class MavenLogger implements Logger {

    private final Mojo mojo;
    private final String name;

    public MavenLogger(final Mojo mojo,
                       final String name) {
        this.mojo = mojo;
        this.name = name;
    }

    @Override
    public void log(final LogLevel level, final String message, final Throwable exception) {
        final var logMessage = String.format("%s: %s", name, message);

        switch (level) {
            case SEVERE -> mojo.getLog().error(logMessage, exception);
            case WARNING -> mojo.getLog().warn(logMessage, exception);
            default -> mojo.getLog().info(logMessage, exception);
        }
    }
}
