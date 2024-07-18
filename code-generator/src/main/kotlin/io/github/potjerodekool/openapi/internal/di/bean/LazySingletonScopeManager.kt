package io.github.potjerodekool.openapi.internal.di.bean

class LazySingletonScopeManager<T>(private val beanDefinition: BeanDefinition) : ScopeManager<T> {
    private var instance: T? = null
    private var isInit = false

    override fun get(applicationContext: DefaultApplicationContext?): T {
        synchronized(this) {
            if (!isInit) {
                isInit = true
                instance = applicationContext!!.createBean(beanDefinition)
            }
        }

        checkNotNull(instance) { String.format("Failed to get instance of %s", beanType!!.name) }

        return instance as (T & Any)
    }

    override val beanType: Class<T>?
        get() {
            val beanType = beanDefinition.beanType ?: return null
            return beanType.java as Class<T>
        }

    /*
    override val beanType: Class<T>?
        get() = {
            if (beanDefinition.beanType != null) beanDefinition.beanType::class.java as Class<T>?
            else null
        }
            */
}
