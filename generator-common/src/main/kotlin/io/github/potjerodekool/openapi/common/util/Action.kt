package io.github.potjerodekool.openapi.common.util

interface Action {
    @Throws(Exception::class)
    fun execute()
}
