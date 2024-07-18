package io.github.potjerodekool.openapi.common.dependency

@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Conditional
annotation class ConditionalOnDependency(val groupId: String, val artifactId: String)
