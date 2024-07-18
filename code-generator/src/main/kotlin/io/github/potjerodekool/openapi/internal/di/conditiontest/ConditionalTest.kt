package io.github.potjerodekool.openapi.internal.di.conditiontest

import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition

fun interface ConditionalTest<C : Annotation> {
    fun test(
        condition: C,
        beanDefinition: BeanDefinition
    ): Boolean
}
