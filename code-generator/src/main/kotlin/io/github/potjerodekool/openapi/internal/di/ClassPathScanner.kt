package io.github.potjerodekool.openapi.internal.di

import io.github.potjerodekool.openapi.internal.di.bean.BeanDefinition
import org.objectweb.asm.ClassReader
import java.io.IOException
import java.util.*

object ClassPathScanner {
    fun scan(): List<BeanDefinition> {
        val beanDefinitions = ArrayList<BeanDefinition>()
        try {
            val resources = classLoader
                .getResources("META-INF/io.github.potjerodekool.openapi.common.autoconfig.AutoConfiguration")

            val iterator = resources.asIterator()

            while (iterator.hasNext()) {
                val resource = iterator.next()

                resource.openStream().use { inputStream ->
                    val lines = String(inputStream.readAllBytes()).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    Arrays.stream(lines)
                        .filter { line: String -> !line.trim { it <= ' ' }.startsWith("#") }
                        .forEach { className: String -> beanDefinitions.addAll(loadConfiguration(className)) }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return beanDefinitions
    }

    private fun loadConfiguration(className: String): List<BeanDefinition> {
        try {
            classLoader.getResourceAsStream(
                className.replace('.', '/') + ".class"
            ).use { inputStream ->
                if (inputStream == null) {
                    return listOf()
                }
                val data = inputStream.readAllBytes()
                val instance = classLoader.loadClass(className).getDeclaredConstructor().newInstance()
                val classReader = ClassReader(data)
                val reader = AutoConfigReader(instance)
                classReader.accept(reader, 0)
                return reader.beanDefinitions
            }
        } catch (e: Exception) {
            //Ignore
            e.printStackTrace();
        }
        return listOf()
    }

    val classLoader: ClassLoader
        get() {
            val classLoader = ClassPathScanner::class.java.classLoader

            return classLoader ?: ClassLoader.getSystemClassLoader()
        }
}
