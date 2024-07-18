package io.github.potjerodekool.openapi.common

import java.util.regex.Pattern

object StatusCodeMatcher {
    private val TWO_X_X: Pattern = Pattern.compile("2[0-9]{2}")

    fun is2XX(status: String?): Boolean {
        return status != null && TWO_X_X.matcher(status).matches()
    }
}
