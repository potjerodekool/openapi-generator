package io.github.potjerodekool.openapi.internal.di.conditiontest;

import io.github.potjerodekool.openapi.common.dependency.ApplicationContext;
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnMissingBean;
import io.github.potjerodekool.openapi.internal.di.DIException;

public class ConditionalOnMissingBeanTest implements ConditionalTest<ConditionalOnMissingBean> {

    private final ApplicationContext applicationContext;

    public ConditionalOnMissingBeanTest(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean test(final ConditionalOnMissingBean condition,
                        final BeanDefinition beanDefinition) {
        try {
            final var clazz = classLoader().loadClass(beanDefinition.className());
            return !applicationContext.isBeanOfTypePresent(clazz);
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private ClassLoader classLoader() {
        final var classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            throw new DIException("No classloader found");
        }
        return classLoader;
    }
}
