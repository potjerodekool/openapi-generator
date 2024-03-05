package io.github.potjerodekool.openapi.common.dependency;

import java.util.Set;

public interface ApplicationContext {

    <T> Set<T> getBeansOfType(Class<T> beanType);

    boolean isBeanOfTypePresent(Class<?> beanType);
}
