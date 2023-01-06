package io.github.potjerodekool.openapi.gradle;

import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;

public class GradleLogger implements Logger {

    private final org.gradle.api.logging.Logger logger;

    public GradleLogger(final Project project) {
        this.logger = project.getLogger();
    }

    @Override
    public void log(final LogLevel level,
                    @Nullable final String message,
                    @Nullable final Throwable exception) {
        logger.log(
                toGradleLogLevel(level),
                message,
                exception
        );
    }

    private org.gradle.api.logging.LogLevel toGradleLogLevel(final LogLevel level) {
        return switch (level) {
            case INFO -> org.gradle.api.logging.LogLevel.INFO;
            case SEVERE -> org.gradle.api.logging.LogLevel.ERROR;
            case WARNING -> org.gradle.api.logging.LogLevel.WARN;
        };
    }
}
