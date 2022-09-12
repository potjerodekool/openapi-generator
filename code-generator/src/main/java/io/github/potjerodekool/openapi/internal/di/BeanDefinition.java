package io.github.potjerodekool.openapi.internal.di;

import java.lang.annotation.Annotation;

public record BeanDefinition(String className,
                             java.util.Map<Class<?>, Annotation> annotations) {

}
