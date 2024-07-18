package io.github.potjerodekool.openapi.internal.di.bean

import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass

class AutoConfigBeanDefinition(
    override val autoConfigInstance: Any,
    private val methodName: String,
    override val beanType: KClass<*>,
    override val annotations: Map<KClass<*>, Annotation>
) : BeanDefinition {

    override val beanMethod: Method?
        get() = Arrays.stream(autoConfigInstance.javaClass.declaredMethods)
            .filter { it: Method -> it.name == methodName }
            .findFirst()
            .orElse(null)
}
