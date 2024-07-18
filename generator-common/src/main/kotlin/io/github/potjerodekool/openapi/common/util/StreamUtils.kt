package io.github.potjerodekool.openapi.common.util

import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport

object StreamUtils {
    fun tryAction(action: Action) {
        try {
            action.execute()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun <E> of(iterator: Iterator<E>): Stream<E> {
        return StreamSupport.stream(IteratorSpliterator(iterator), false)
    }

    private class IteratorSpliterator<E>(private val iterator: Iterator<E>) : Spliterator<E> {
        override fun tryAdvance(action: Consumer<in E>): Boolean {
            if (iterator.hasNext()) {
                val next = iterator.next()
                action.accept(next)
                return true
            } else {
                return false
            }
        }

        override fun trySplit(): Spliterator<E>? {
            return null
        }

        override fun estimateSize(): Long {
            return Long.MAX_VALUE
        }

        override fun characteristics(): Int {
            return 0
        }
    }
}


