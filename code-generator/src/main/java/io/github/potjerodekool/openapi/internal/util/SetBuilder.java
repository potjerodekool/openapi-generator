package io.github.potjerodekool.openapi.internal.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SetBuilder<T> {

    private final Set<T> values;

    public SetBuilder() {
        this.values = new HashSet<>();
    }

    public SetBuilder(final Set<T> values) {
        this.values = values;
    }

    public SetBuilder<T> add(final T value) {
        this.values.add(value);
        return this;
    }

    public Set<T> build() {
        return this.values;
    }

    public <R> SetBuilder<R> map(final Function<T, R> mapper) {
        final var mappedValues = this.values.stream()
                .map(mapper)
                .collect(Collectors.toSet());
        return new SetBuilder<>(mappedValues);
    }

}