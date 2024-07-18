package io.github.potjerodekool.openapi.common

import io.github.potjerodekool.codegen.Language

data class GeneratorConfig(
    val language: Language,
    val basePackageName: String,
    val features: Map<String, Boolean>
) {
    fun configPackageName(): String {
        return "$basePackageName.config"
    }

    fun isFeatureEnabled(feature: String): Boolean {
        return java.lang.Boolean.TRUE == features[feature]
    }
}
