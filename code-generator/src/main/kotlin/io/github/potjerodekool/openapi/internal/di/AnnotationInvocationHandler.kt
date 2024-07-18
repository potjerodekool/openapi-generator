package io.github.potjerodekool.openapi.internal.di

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

internal class AnnotationInvocationHandler(
    private val annotationClass: Class<*>,
    private val attributes: Map<String, Any>
) : InvocationHandler {
    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
        val methodName = method.name

        if (method.isDefault) {
            return InvocationHandler.invokeDefault(proxy, method, args)
        }

        return when (methodName) {
            "annotationType" -> annotationClass
            "equals" -> !args.isNullOrEmpty() && this == args[0]
            "hashCode" -> this.hashCode()
            "toString" -> this.toString()
            else -> {
                if (method.returnType == Void::class.java || method.returnType == Void.TYPE) {
                    return null
                } else if (method.parameterCount == 0) {
                    if (attributes.containsKey(methodName)) {
                        return attributes[methodName]
                    }
                }
                null
            }
        }!!
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("@")
        sb.append(annotationClass.name)
        sb.append("(")

        val attributeJoiner = StringJoiner(",")

        attributes.forEach { (key: String, value: Any) ->
            attributeJoiner.add(
                quoteString(key).toString() + "=" + quoteString(
                    value
                )
            )
        }

        sb.append(attributeJoiner)
        sb.append(")")

        return sb.toString()
    }

    private fun quoteString(value: Any): Any {
        return if (value is String) {
            "\"" + value + "\""
        } else {
            value
        }
    }
}
