package io.github.potjerodekool.openapi.internal.util;

import static io.github.potjerodekool.codegen.model.util.StringUtils.firstUpper;

public final class StringUtils {

    private StringUtils() {
    }

    public static String toValidClassName(final String name) {
        var className = name;
        className = upperFirst(className.split("-"));
        return upperFirst(className.split("_"));

    }

    private static String upperFirst(final String[] parts) {
        final var sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            sb.append(firstUpper(parts[i]));
        }

        return sb.toString();
    }
}
