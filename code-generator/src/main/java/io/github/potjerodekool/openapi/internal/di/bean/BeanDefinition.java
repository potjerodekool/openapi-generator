package io.github.potjerodekool.openapi.internal.di.bean;

import java.lang.annotation.Annotation;

public record BeanDefinition(String className,
                             java.util.Map<Class<?>, Annotation> annotations) {

}
