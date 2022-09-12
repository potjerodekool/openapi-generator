package io.github.potjerodekool.openapi.internal.di.scope;

import io.github.potjerodekool.openapi.internal.di.ApplicationContext;

public interface ScopeManager<T> {

    Class<T> getBeanType();

    T get(ApplicationContext applicationContext);
}
