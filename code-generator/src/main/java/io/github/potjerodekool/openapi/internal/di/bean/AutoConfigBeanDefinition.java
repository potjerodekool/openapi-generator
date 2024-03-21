package io.github.potjerodekool.openapi.internal.di.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class AutoConfigBeanDefinition implements BeanDefinition {

    private final Object autoConfigInstance;
    private final String methodName;
    private final Class<?> type;
    private final Map<Class<?>, Annotation> annotations;

    public AutoConfigBeanDefinition(final Object autoConfigInstance,
                                    final String methodName,
                                    final Class<?> type,
                                    final Map<Class<?>, Annotation> annotations) {
        this.autoConfigInstance = autoConfigInstance;
        this.methodName = methodName;
        this.type = type;
        this.annotations = annotations;
    }

    @Override
    public Object getAutoConfigInstance() {
        return autoConfigInstance;
    }

    @Override
    public Map<Class<?>, Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public Class<?> getBeanType() {
        return type;
    }

    @Override

    public Method getBeanMethod() {
        return Arrays.stream(autoConfigInstance.getClass().getDeclaredMethods())
                .filter(it -> it.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }
}
