package io.github.potjerodekool.openapi.common.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtils {

    private StreamUtils() {
    }

    public static void tryAction(final Action action) {
        try {
            action.execute();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <E> Stream<E> of(final Iterator<E> iterator) {
        return StreamSupport.stream(new IteratorSpliterator<>(iterator), false);
    }

    private static class IteratorSpliterator<E> implements Spliterator<E> {

        private final Iterator<E> iterator;

        IteratorSpliterator(final Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super E> action) {
            if (iterator.hasNext()) {
                final var next = iterator.next();
                action.accept(next);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public @Nullable Spliterator<E> trySplit() {
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
}


