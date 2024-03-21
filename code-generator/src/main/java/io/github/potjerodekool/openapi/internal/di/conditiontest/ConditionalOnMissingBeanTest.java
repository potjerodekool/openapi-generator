package io.github.potjerodekool.openapi.internal.di.conditiontest;

import io.github.potjerodekool.openapi.common.dependency.ApplicationContext;
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition;
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnMissingBean;

public class ConditionalOnMissingBeanTest implements ConditionalTest<ConditionalOnMissingBean> {

    private final ApplicationContext applicationContext;

    public ConditionalOnMissingBeanTest(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean test(final ConditionalOnMissingBean condition,
                        final BeanDefinition beanDefinition) {
        return !applicationContext.isBeanOfTypePresent(condition.type());
    }
}
