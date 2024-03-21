package io.github.potjerodekool.openapi.internal.di.bean;

import org.checkerframework.checker.nullness.qual.Nullable;

public class LazySingletonScopeManager<T> implements ScopeManager<T> {

    private final BeanDefinition beanDefinition;
    private @Nullable T instance = null;
    private boolean isInit = false;

    public LazySingletonScopeManager(final BeanDefinition beanDefinition) {
        this.beanDefinition = beanDefinition;
    }

    @Override
    public T get(final DefaultApplicationContext applicationContext) {
        synchronized (this) {
            if (!isInit) {
                isInit = true;
                instance = applicationContext.createBean(beanDefinition);
            }
        }

        if (instance == null) {
            throw new IllegalStateException(String.format("Failed to get instance of %s", getBeanType().getName()));
        }

        return instance;
    }

    @Override
    public Class<T> getBeanType() {
        return (Class<T>) beanDefinition.getBeanType();
    }
}
