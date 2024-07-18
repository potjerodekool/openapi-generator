package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.openapi.common.generate.annotation.TypeInfo
import java.util.function.Supplier

object ExtensionsHelper {

    @JvmStatic
    fun <T> getExtension(
        extensions: MutableMap<String, Any?>?,
        name: String?,
        resultClass: Class<T>
    ): T? {
        if (extensions == null || extensions.isEmpty()) {
            return getDefaultValue(resultClass)
        }

        val extension = extensions[name] ?: return getDefaultValue(resultClass)

        return extension as T
    }

    fun <T> getExtension(
        extensions: MutableMap<String, Any?>?,
        name: String?,
        returnTypeInfo: TypeInfo<T>?
    ): T? {
        return getExtension(extensions, name, returnTypeInfo, null)
    }

    fun <T> getExtension(
        extensions: MutableMap<String, Any?>?,
        name: String?,
        returnTypeInfo: TypeInfo<T>?,
        defaultValueSupplier: Supplier<T>?
    ): T? {
        if (extensions == null
            || extensions.isEmpty()
        ) {
            return defaultValueSupplier?.get()
        }

        val extension = extensions[name]
            ?: return defaultValueSupplier?.get()

        return extension as T
    }

    private fun <T> getDefaultValue(resultClass: Class<T>): T? {
        return if (resultClass == MutableList::class.java) {
            listOf<Any>() as T
        } else {
            null
        }
    }
}
