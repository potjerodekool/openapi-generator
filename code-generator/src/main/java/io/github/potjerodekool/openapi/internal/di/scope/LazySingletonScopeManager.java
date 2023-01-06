package io.github.potjerodekool.openapi.internal.di.scope;

import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LazySingletonScopeManager<T> implements ScopeManager<T> {

    private final Class<T> beanClass;
    private @Nullable T instance = null;
    private boolean isInit = false;

    public LazySingletonScopeManager(final Class<T> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public T get(final ApplicationContext applicationContext) {
        synchronized (this) {
            if (!isInit) {
                isInit = true;
                instance = applicationContext.createBean(beanClass);
            }
        }

        if (instance == null) {
            throw new IllegalStateException(String.format("Failed to get instance of %s", beanClass.getName()));
        }

        return instance;
    }

    @Override
    public Class<T> getBeanType() {
        return beanClass;
    }
}
