package io.github.potjerodekool.openapi.common.util

import io.github.potjerodekool.codegen.model.util.StringUtils
import java.util.*

object StringUtils {
    @JvmStatic
    fun toValidClassName(name: String): String {
        var className = name
        className = upperFirst(className.split("-".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray())
        return upperFirst(className.split("_".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray())
    }

    private fun upperFirst(parts: Array<String>): String {
        val sb = StringBuilder()

        for (part in parts) {
            sb.append(StringUtils.firstUpper(part))
        }

        return sb.toString()
    }

    fun toSnakeCase(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return value
        } else {
            val builder = StringBuilder()

            for (c in value.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    builder.append('_')
                    builder.append(c.lowercaseChar())
                } else {
                    builder.append(c)
                }
            }
            return builder.toString()
        }
    }

    fun toUpperCase(value: String?): String? {
        return if (value.isNullOrEmpty()) {
            value
        } else {
            value.uppercase(Locale.getDefault())
        }
    }
}
