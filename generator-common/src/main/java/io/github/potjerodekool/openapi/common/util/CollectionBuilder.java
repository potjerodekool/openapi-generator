package io.github.potjerodekool.openapi.common.util;

import java.util.*;
import java.util.function.Function;

public class CollectionBuilder<T> {

    private final List<T> list = new ArrayList<>();

    public CollectionBuilder() {
    }

    public CollectionBuilder(final List<T> values) {
        this.list.addAll(values);
    }

    public CollectionBuilder<T> add(final T value) {
        this.list.add(value);
        return this;
    }

    public CollectionBuilder<T> addAll(final Collection<T> values) {
        this.list.addAll(values);
        return this;
    }

    public <R> CollectionBuilder<R> map(final Function<T, R> mapper) {
        final var mappedValues = this.list.stream()
                .map(mapper)
                .toList();
        return new CollectionBuilder<>(mappedValues);
    }

    public List<T> buildList() {
        return list;
    }

    public Set<T> buildSet() {
        return new HashSet<>(list);
    }
}
