package io.github.potjerodekool.openapi.common.dependency

import kotlin.reflect.KClass

interface ApplicationContext {
    fun <T> getBeansOfType(beanType: Class<T>?): Set<T>

    fun isBeanOfTypePresent(beanType: Class<*>?): Boolean

    fun isBeanOfTypePresent(beanType: KClass<*>?): Boolean
}
