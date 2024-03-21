package io.github.potjerodekool.openapi.common.util;

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

    public static String toSnakeCase(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        } else {
            final var builder = new StringBuilder();

            for (final char c : value.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    builder.append('_');
                    builder.append(Character.toLowerCase(c));
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }
    }

    public static String toUpperCase(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        } else {
            return value.toUpperCase();
        }
    }
}
