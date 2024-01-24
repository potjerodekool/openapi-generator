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

        for (final String part : parts) {
            sb.append(firstUpper(part));
        }

        return sb.toString();
    }
}
