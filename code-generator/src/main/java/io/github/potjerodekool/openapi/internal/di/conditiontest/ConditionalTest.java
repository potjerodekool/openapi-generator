package io.github.potjerodekool.openapi.internal.di.conditiontest;

import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;

import java.lang.annotation.Annotation;

@FunctionalInterface
public interface ConditionalTest<C extends Annotation> {

    boolean test(C condition,
                 BeanDefinition beanDefinition);
}
