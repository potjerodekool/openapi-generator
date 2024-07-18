package io.github.potjerodekool.openapi.internal.di

import io.github.potjerodekool.openapi.common.dependency.Bean
import io.github.potjerodekool.openapi.internal.di.ClassPathScanner.classLoader
import io.github.potjerodekool.openapi.internal.di.bean.AutoConfigBeanDefinition
import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition
import org.objectweb.asm.*
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class AutoConfigReader(val autoConfigInstance: Any) : ClassVisitor(Opcodes.ASM9) {
    private val _beanDefinitions: MutableList<BeanDefinition> = ArrayList()

    val beanDefinitions: List<BeanDefinition>
        get() = _beanDefinitions

    override fun visitMethod(
        access: Int, name: String,
        descriptor: String,
        signature: String?, exceptions: Array<String>?
    ): MethodVisitor {
        return AutoConfigMethodReader(api, name ,descriptor, this)
    }

    fun addBeanDefinition(beanDefinition: BeanDefinition) {
        _beanDefinitions.add(beanDefinition)
    }
}

internal class AutoConfigMethodReader(
    api: Int,
    private val methodName: String,
    private val descriptor: String,
    private val autoConfigReader: AutoConfigReader
) : MethodVisitor(api) {
    private val annotations: MutableMap<KClass<*>, Annotation> = HashMap()

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        val className = descriptorToClassName(descriptor)
        return AutoConfigAnnotationReader(api, className, this)
    }

    private fun descriptorToClassName(descriptor: String): String {
        var className = descriptor.substring(1)
        className = className.substring(0, className.length - 1)
        return className.replace('/', '.')
    }

    fun addAnnotation(annotationClass: KClass<*>, annotation: Annotation) {
        annotations[annotationClass] = annotation
    }

    override fun visitEnd() {
        if (annotations.containsKey(Bean::class)) {
            try {
                val returnType = javaClass.classLoader.loadClass(
                    Type.getMethodType(
                        descriptor
                    ).returnType.className
                ).kotlin

                val beanDefinition = AutoConfigBeanDefinition(
                    autoConfigReader.autoConfigInstance,
                    methodName,
                    returnType,
                    annotations
                )
                autoConfigReader.addBeanDefinition(beanDefinition)
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }
}

internal class AutoConfigAnnotationReader(
    api: Int,
    private val className: String,
    private val methodReader: AutoConfigMethodReader
) : AnnotationVisitor(api) {
    private val attributes: MutableMap<String, Any> = HashMap()

    override fun visit(name: String, value: Any) {
        if (value is Type) {
            try {
                val clazz = javaClass.classLoader.loadClass(
                    value.className
                )
                attributes[name] = clazz
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            }
        } else {
            attributes[name] = value
        }
    }

    override fun visitEnd() {
        val classLoader = classLoader
        val annotationClass: Class<*>

        try {
            annotationClass = classLoader.loadClass(className)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        }

        val annotation = Proxy.newProxyInstance(
            classLoader,
            arrayOf(annotationClass),
            AnnotationInvocationHandler(
                annotationClass,
                this.attributes
            )
        ) as Annotation

        methodReader.addAnnotation(annotationClass.kotlin, annotation)
    }
}