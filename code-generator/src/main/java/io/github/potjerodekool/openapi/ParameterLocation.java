package io.github.potjerodekool.openapi;

import java.util.Arrays;

/**
 Enumeration of where a parameter can be located.
 */
public enum ParameterLocation {
    PATH("path"),
    QUERY("query"),
    HEADER("header"),
    COOKIE("cookie");

    private final String value;

    ParameterLocation(final String value) {
        this.value = value;
    }

    public static ParameterLocation parseIn(final String in) {
        return Arrays.stream(values())
                .filter(pl -> pl.value.equals(in))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(in));
    }
}
