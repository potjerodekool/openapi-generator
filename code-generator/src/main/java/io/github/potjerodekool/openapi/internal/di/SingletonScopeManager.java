package io.github.potjerodekool.openapi.internal.di;

import org.checkerframework.checker.nullness.qual.NonNull;

public class SingletonScopeManager<T> implements ScopeManager<T> {

    private final @NonNull T instance;

    public SingletonScopeManager(final @NonNull T instance) {
        this.instance = instance;
    }

    @Override
    public T get(final DefaultApplicationContext applicationContext) {
        return instance;
    }

    @Override
    public Class<T> getBeanType() {
        return (Class<T>) instance.getClass();
    }
}
