package io.github.potjerodekool.openapi.internal.di.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface BeanDefinition {

    Object getAutoConfigInstance();

    java.util.Map<Class<?>, Annotation> getAnnotations();

    Class<?> getBeanType();

    Method getBeanMethod();
}
