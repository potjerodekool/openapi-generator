package io.github.potjerodekool.openapi.common.dependency

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Conditional
annotation class ConditionalOnMissingBean(val type: KClass<*>)
