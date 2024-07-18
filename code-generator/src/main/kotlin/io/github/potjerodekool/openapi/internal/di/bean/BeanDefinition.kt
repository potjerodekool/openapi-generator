package io.github.potjerodekool.openapi.internal.di.bean

import java.lang.reflect.Method
import kotlin.reflect.KClass

interface BeanDefinition {
    val autoConfigInstance: Any?
    val annotations: Map<KClass<*>, Annotation>
    val beanType: KClass<*>?
    val beanMethod: Method?
}
