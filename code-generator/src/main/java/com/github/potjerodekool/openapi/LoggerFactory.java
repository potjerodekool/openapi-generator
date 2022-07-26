package com.github.potjerodekool.openapi;

import java.util.function.Function;

public class LoggerFactory {

    private static final LoggerFactory FACTORY = new LoggerFactory();

    private Function<String, Logger> loggerProvider = DefaultLogger::new;

    private LoggerFactory() {
    }

    public static void setLoggerProvider(Function<String, Logger> loggerProvider) {
        FACTORY.loggerProvider = loggerProvider;
    }

    public static Logger getLogger(final String name) {
        return FACTORY.loggerProvider.apply(name);
    }

}
