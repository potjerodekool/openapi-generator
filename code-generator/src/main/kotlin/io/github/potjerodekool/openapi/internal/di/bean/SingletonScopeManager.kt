package io.github.potjerodekool.openapi.internal.di.bean

class SingletonScopeManager<T>(private val instance: T) : ScopeManager<T> {
    override fun get(applicationContext: DefaultApplicationContext?): T {
        return instance
    }

    override val beanType: Class<T>
        get() = instance!!::class.java as Class<T>
}
