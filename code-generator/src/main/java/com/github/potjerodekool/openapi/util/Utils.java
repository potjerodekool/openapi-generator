package com.github.potjerodekool.openapi.util;

import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

public final class Utils {

    private Utils() {
    }

    public static <T> T requireNonNull(final @Nullable T value) throws RuntimeException {
        return requireNonNull(value, () -> new NullPointerException("required nonnull value"));
    }

    public static <T, E extends Exception> T requireNonNull(final @Nullable T value,
                                                            final Supplier<E> exceptionSupplier) throws E {
        if (value == null) {
            throw exceptionSupplier.get();
        } else {
            return value;
        }
    }

    public static @Nullable String getCreateRef(final Schema schema) {
        final var schemaImpl = (SchemaImpl) schema;
        final var createRef = schemaImpl._getCreatingRef();
        return createRef != null ? createRef.getRefString() : null;
    }

    public static String firstUpper(final String value) {
        if (value.length() < 1) {
            return value;
        } else {
            final var first = Character.toUpperCase(value.charAt(0));

            return value.length() == 1
                    ? Character.toString(first)
                    : first + value.substring(1);
        }
    }

    public static Pair<String, String> resolvePackageNameAndName(final String path) {
        final var packageSepIndex = path.lastIndexOf('/');

        if (packageSepIndex < 0) {
            final var nameSep = path.lastIndexOf('.');
            final var name = nameSep > 0 ? path.substring(0, nameSep) : path;
            return new Pair<>("", name);
        } else {
            final var packageName = path.substring(0, packageSepIndex).replace('/', '.');
            final var nameSep = path.lastIndexOf('.');
            final var name = nameSep > 0 ? path.substring(packageSepIndex + 1, nameSep) : path.substring(packageSepIndex + 1);
            return new Pair<>(packageName, name);
        }
    }

    public static boolean isNullOrEmpty(final @Nullable String value) {
        return value == null || value.length() == 0;
    }

}
