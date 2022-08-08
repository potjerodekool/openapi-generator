package io.github.potjerodekool.openapi.tree.enums;

import java.util.Arrays;

public enum OpenApiSecuritySchemeIn {

    DEFAULT(""),
    HEADER("header"),
    QUERY("query"),
    COOKIE("cookie");

    private final String value;

    OpenApiSecuritySchemeIn(final String value) {
        this.value = value;
    }

    public static OpenApiSecuritySchemeIn fromValue(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No enum constant %s", value)));
    }
}
