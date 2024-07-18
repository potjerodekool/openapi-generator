package io.github.potjerodekool.openapi.common.util

object CollectionUtils {
    @JvmStatic
    fun <T> nonNull(list: List<T>?): List<T> {
        return list ?: ArrayList()
    }

    @JvmStatic
    fun <K, V> nonNull(map: Map<K, V>?): Map<K, V> {
        return map ?: HashMap()
    }
}
