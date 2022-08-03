package io.github.potjerodekool.openapi;

/**
 Enumeration of where a parameter can be located.
 */
public enum ParameterLocation {
    QUERY,
    PATH;

    public static ParameterLocation parseIn(final String in) {
        return switch (in) {
            case "query" -> QUERY;
            case "path" -> PATH;
            default -> throw new IllegalArgumentException(in);
        };
    }
}
