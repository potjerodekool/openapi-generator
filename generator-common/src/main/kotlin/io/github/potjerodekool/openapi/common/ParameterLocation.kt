package io.github.potjerodekool.openapi.common

import java.util.*

/**
 * Enumeration of where a parameter can be located.
 */
enum class ParameterLocation(private val value: String) {
    PATH("path"),
    QUERY("query"),
    HEADER("header"),
    COOKIE("cookie");

    fun value(): String {
        return value
    }

    companion object {
        fun parseIn(`in`: String): ParameterLocation {
            return Arrays.stream(values())
                .filter { pl: ParameterLocation -> pl.value == `in` }
                .findFirst()
                .orElseThrow { IllegalArgumentException(`in`) }
        }
    }
}
