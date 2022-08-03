package io.github.potjerodekool.openapi.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
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

    public static <T> List<@NonNull T> requiresNonNullList(final List<T> list) {
        final @NonNull @Initialized List<@NonNull T> newList = new ArrayList<@NonNull T>();

        list.forEach(item -> {
            if (item == null) {
                throw new NullPointerException("List contains null element");
            }
            newList.add(item);
        });

        return newList;
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

    public static QualifiedName resolveQualified(final String path) {
        final var packageSepIndex = path.lastIndexOf('/');

        if (packageSepIndex < 0) {
            final var nameSep = path.lastIndexOf('.');
            final var name = nameSep > 0 ? path.substring(0, nameSep) : path;
            return new QualifiedName("", name);
        } else {
            final var packageName = path.substring(0, packageSepIndex).replace('/', '.');
            final var nameSep = path.lastIndexOf('.');
            final var name = nameSep > 0 ? path.substring(packageSepIndex + 1, nameSep) : path.substring(packageSepIndex + 1);
            return new QualifiedName(packageName, name);
        }
    }

    public static boolean isNullOrEmpty(final @Nullable String value) {
        return value == null || value.length() == 0;
    }

    public static NodeList<? extends Node> asGeneric(@SuppressWarnings("rawtypes") final NodeList n) {
        return ((NodeList<? extends Node>) n);
    }
}
