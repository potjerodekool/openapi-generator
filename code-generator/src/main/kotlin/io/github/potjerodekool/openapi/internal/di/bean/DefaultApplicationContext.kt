package io.github.potjerodekool.openapi.internal.di.bean

import io.github.potjerodekool.openapi.common.dependency.*
import io.github.potjerodekool.openapi.internal.di.DIException
import io.github.potjerodekool.openapi.internal.di.conditiontest.ConditionalOnDependencyTest
import io.github.potjerodekool.openapi.internal.di.conditiontest.ConditionalOnMissingBeanTest
import io.github.potjerodekool.openapi.internal.di.conditiontest.ConditionalTest
import jakarta.inject.Inject
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

class DefaultApplicationContext(dependencyChecker: DependencyChecker) : ApplicationContext {
    private val beansMaps: MutableMap<Class<*>?, MutableList<ScopeManager<*>>> = HashMap()

    private val conditionTests: MutableMap<KClass<*>, ConditionalTest<*>> = HashMap()

    init {
        add(ConditionalOnDependency::class, ConditionalOnDependencyTest(dependencyChecker))
        add(
            ConditionalOnMissingBean::class, ConditionalOnMissingBeanTest(
                this
            )
        )
    }

    private fun <A : Annotation> add(
        annotationType: KClass<out Annotation>,
        test: ConditionalTest<A>
    ) {
        conditionTests[annotationType] = test
    }

    override fun isBeanOfTypePresent(beanType: Class<*>?): Boolean {
        return beanType != null && beansMaps.values.stream().flatMap { obj: List<ScopeManager<*>> -> obj.stream() }
            .anyMatch { sm: ScopeManager<*> -> beanType.isAssignableFrom(sm.beanType) }
    }

    override fun isBeanOfTypePresent(beanType: KClass<*>?): Boolean {
        return beanType != null && isBeanOfTypePresent(beanType::class.java)
    }

    fun <T> registerBean(bean: T) {
        registerBean(bean!!::class.java, bean)
    }

    fun <T> registerBeans(beanDefinitions: List<BeanDefinition>) {
        beanDefinitions.stream()
            .filter { beanDefinition: BeanDefinition -> !this.hasConditions(beanDefinition) }
            .forEach { beanDefinition: BeanDefinition -> this.doRegister(beanDefinition) }

        beanDefinitions.stream()
            .filter { beanDefinition: BeanDefinition -> this.hasConditions(beanDefinition) }
            .filter { beanDefinition: BeanDefinition -> this.testConditions(beanDefinition) }
            .forEach { beanDefinition: BeanDefinition -> this.doRegister(beanDefinition) }
    }

    private fun doRegister(beanDefinition: BeanDefinition) {
        val sm = createLazySingletonScopeManager(beanDefinition)
        if (sm != null) {
            val list = beansMaps.computeIfAbsent(sm.beanType) { key: Class<*>? -> ArrayList() }
            list.add(sm)
        }
    }

    private fun createLazySingletonScopeManager(beanDefinition: BeanDefinition): LazySingletonScopeManager<*>? {
        return LazySingletonScopeManager<Any>(beanDefinition)
    }

    private fun testConditions(beanDefinition: BeanDefinition): Boolean {
        var result = true
        val annotationIterator: Iterator<Annotation> = beanDefinition.annotations.values.iterator()

        while (result && annotationIterator.hasNext()) {
            val annotation = annotationIterator.next()
            val annotationTyp = annotation.annotationClass

            if (isCondition(annotationTyp)) {
                val test = conditionTests[annotationTyp] as ConditionalTest<Annotation>?
                if (test != null) {
                    if (!test.test(annotation, beanDefinition)) {
                        result = false
                    }
                }
            }
        }

        return result
    }

    private fun hasConditions(beanDefinition: BeanDefinition): Boolean {
        return beanDefinition.annotations.values.stream()
            .anyMatch { annotation: Annotation -> isCondition(annotation.annotationClass) }
    }

    private fun isCondition(annotation: KClass<out Annotation>): Boolean {
        return annotation.hasAnnotation<Conditional>()
    }

    fun <T> registerBean(
        beanClass: Class<*>?,
        bean: T
    ) {
        val list = beansMaps.computeIfAbsent(beanClass) { key: Class<*>? -> ArrayList() }
        list.add(SingletonScopeManager(bean))
    }

    private fun <T> getBeanOfType(beanType: Class<T>): T {
        return resolveBean(beanType)
    }

    override fun <T> getBeansOfType(beanType: Class<T>?): Set<T> {
        return resolveBeans(beanType)
    }

    private fun <T> resolveBean(beanType: Class<T>): T {
        val resolvedBeans = resolveBeans(beanType)

        if (resolvedBeans.isEmpty()) {
            throw DIException(String.format("Failed to resolve bean of %s", beanType.name))
        } else if (resolvedBeans.size > 1) {
            throw DIException(
                String.format(
                    "Failed to resolve unique bean of %s. Found %s beans",
                    beanType.name,
                    resolvedBeans.size
                )
            )
        } else {
            return resolvedBeans.iterator().next()
        }
    }

    private fun <T> resolveBeans(beanType: Class<T>?): Set<T> {
        if (beanType == null) {
            return emptySet()
        }

        val list: List<ScopeManager<*>>? = beansMaps[beanType]

        val resolvedBeans: MutableSet<ScopeManager<*>> = if (list != null) {
            HashSet(list)
        } else {
            HashSet()
        }

        for (aClass in beansMaps.keys) {
            if (beanType.isAssignableFrom(aClass)) {
                resolvedBeans.addAll(beansMaps[aClass]!!)
            }
        }

        return resolvedBeans.stream()
            .map { sm: ScopeManager<*> -> sm.get(this) as T }
            .collect(Collectors.toSet())
    }

    private fun classLoader(): ClassLoader {
        val classLoader = javaClass.classLoader ?: throw DIException("No classloader found")
        return classLoader
    }

    fun <T> createBean(beanDefinition: BeanDefinition): T {
        val arguments = resolveArguments(beanDefinition.beanMethod)
        try {
            val instance = beanDefinition.autoConfigInstance
            val method = beanDefinition.beanMethod
            val bean = method!!.invoke(instance, *arguments) as T
            registerBean(bean!!::class.java, bean)
            return bean
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun resolveArguments(executable: Executable?): Array<Any?> {
        val arguments: Array<Any?>

        if (executable!!.parameterCount == 0) {
            arguments = arrayOfNulls(0)
        } else {
            return Arrays.stream(executable.parameterTypes)
                .map { beanType -> this.getBeanOfType(beanType) }
                .toArray()
        }

        return arguments
    }

    private fun <T> resolveConstructor(clazz: Class<T>): Optional<Constructor<T>> {
        val declaredConstructors = clazz.declaredConstructors as Array<Constructor<T>>

        return Optional.ofNullable(
            Arrays.stream(declaredConstructors)
                .filter { constructor: Constructor<T> ->
                    constructor.isAnnotationPresent(
                        Inject::class.java
                    )
                }
                .findFirst()
                .orElseGet {
                    try {
                        return@orElseGet clazz.getConstructor()
                    } catch (e: NoSuchMethodException) {
                        return@orElseGet null
                    }
                }
        )
    }
}
