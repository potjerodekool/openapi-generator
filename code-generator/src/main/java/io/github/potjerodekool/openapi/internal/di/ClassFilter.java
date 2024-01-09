package io.github.potjerodekool.openapi.internal.di;

public interface ClassFilter {
    boolean filter(BeanDefinition beanDefinition);
}
