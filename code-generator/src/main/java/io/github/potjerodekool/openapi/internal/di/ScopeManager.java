package io.github.potjerodekool.openapi.internal.di;

import io.github.potjerodekool.openapi.internal.di.DefaultApplicationContext;

public interface ScopeManager<T> {

    Class<T> getBeanType();

    T get(DefaultApplicationContext applicationContext);
}
