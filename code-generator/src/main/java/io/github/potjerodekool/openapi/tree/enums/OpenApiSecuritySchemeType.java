package io.github.potjerodekool.openapi.tree.enums;

import java.util.Arrays;

public enum OpenApiSecuritySchemeType {

    DEFAULT(""),
    APIKEY("apiKey"),
    HTTP("http"),
    OPENIDCONNECT("openIdConnect"),
    OAUTH2("oauth2");

    private final String value;

    OpenApiSecuritySchemeType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OpenApiSecuritySchemeType fromValue(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No enum constant %s", value)));
    }
}
