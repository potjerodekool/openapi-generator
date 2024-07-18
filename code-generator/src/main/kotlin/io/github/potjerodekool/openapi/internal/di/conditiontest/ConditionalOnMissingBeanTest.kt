package io.github.potjerodekool.openapi.internal.di.conditiontest

import io.github.potjerodekool.openapi.common.dependency.ApplicationContext
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnMissingBean
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition

class ConditionalOnMissingBeanTest(private val applicationContext: ApplicationContext) :
    ConditionalTest<ConditionalOnMissingBean> {
    override fun test(
        condition: ConditionalOnMissingBean,
        beanDefinition: BeanDefinition
    ): Boolean {
        return !applicationContext.isBeanOfTypePresent(condition.type)
    }
}
