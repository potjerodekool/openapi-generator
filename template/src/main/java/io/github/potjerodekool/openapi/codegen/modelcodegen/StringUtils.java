package io.github.potjerodekool.openapi.codegen.modelcodegen;

public final class StringUtils {

    private StringUtils() {
    }

    public static String firstUpper(final String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
