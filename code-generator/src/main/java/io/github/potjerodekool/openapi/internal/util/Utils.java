package io.github.potjerodekool.openapi.internal.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
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

    public static String getOrDefault(final @Nullable String value,
                                      final String defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static @Nullable String getCreateRef(final Schema schema) {
        final var schemaImpl = (SchemaImpl) schema;
        final var createRef = schemaImpl._getCreatingRef();
        return createRef != null ? createRef.getRefString() : null;
    }

    public static String firstUpper(final String value) {
        return replaceFirst(value, Character::toUpperCase);
    }

    public static String firstLower(final String value) {
        return replaceFirst(value, Character::toLowerCase);
    }

    private static String replaceFirst(final String value,
                                       final Function<Character, Character> replaceFunction) {
        if (value.length() < 1) {
            return value;
        } else {
            final var first = replaceFunction.apply(value.charAt(0));

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

    public static boolean isFalse(final @Nullable Boolean value) {
        if (value == null) {
            return false;
        }
        return Boolean.FALSE.equals(value);
    }

    public static boolean isTrue(final @Nullable Boolean value) {
        if (value == null) {
            return false;
        }
        return Boolean.TRUE.equals(value);
    }

    public static String toUriString(final File file) {
        try {
            return file.getCanonicalFile().toURI().toString();
        } catch (final IOException e) {
            throw new GenerateException(e);
        }
    }

    public static <T> Spliterator<T> spliterator(final Iterator<T> iterator) {
        return new SimpleSpliterator<>(iterator);
    }
}

class SimpleSpliterator<T> implements Spliterator<T> {

    private final Iterator<T> iterator;

    SimpleSpliterator(final Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
        if (iterator.hasNext()) {
            action.accept(iterator.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    @SuppressWarnings("return")
    public @Nullable Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}