package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.openapi.common.generate.model.Digits
import java.util.*

object ValidationExtensions {
    const val ASSERT: String = "x-assert"

    @JvmStatic
    fun digits(extensions: Map<String?, Any>?): Optional<Digits> {
        if (extensions == null) {
            return Optional.empty()
        }

        val validation = getValidation(extensions)
        val digits = validation["digits"] as? Map<*, *>
            ?: return Optional.empty()

        val digitsMap = digits as Map<String, Any>

        val integer = digitsMap["integer"] as Int?
        val fraction = digitsMap["fraction"] as Int?
        return Optional.of(
            Digits(
                integer!!, fraction!!
            )
        )
    }

    @JvmStatic
    fun allowedValue(extensions: Map<String?, Any>?): Any? {
        val validation = getValidation(extensions)
        return validation["allowed-value"]
    }

    @JvmStatic
    fun getValidation(extensions: Map<String?, Any>?): Map<String, Any> {
        return if (extensions != null
        ) extensions.getOrDefault("x-validation", java.util.Map.of<Any, Any>()) as Map<String, Any>
        else java.util.Map.of()
    }
}
