package io.github.potjerodekool.openapi.common.util

import java.util.function.Function

class CollectionBuilder<T> {
    private val list: MutableList<T> = ArrayList()

    constructor()

    constructor(values: List<T>?) {
        list.addAll(values!!)
    }

    fun add(value: T): CollectionBuilder<T> {
        list.add(value)
        return this
    }

    fun addAll(values: Collection<T>?): CollectionBuilder<T> {
        list.addAll(values!!)
        return this
    }

    fun <R> map(mapper: Function<T, R>?): CollectionBuilder<R> {
        val mappedValues = list.stream()
            .map(mapper)
            .toList()
        return CollectionBuilder(mappedValues)
    }

    fun buildList(): List<T> {
        return list
    }

    fun buildSet(): Set<T> {
        return HashSet(list)
    }
}
