package io.github.potjerodekool.openapi.internal.di.bean

interface ScopeManager<T> {
    val beanType: Class<T>?

    fun get(applicationContext: DefaultApplicationContext?): T
}
