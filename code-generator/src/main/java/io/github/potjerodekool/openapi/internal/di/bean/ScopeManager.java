package io.github.potjerodekool.openapi.internal.di.bean;

public interface ScopeManager<T> {

    Class<T> getBeanType();

    T get(DefaultApplicationContext applicationContext);
}
