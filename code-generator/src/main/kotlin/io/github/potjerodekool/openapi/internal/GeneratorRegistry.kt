package io.github.potjerodekool.openapi.internal

import io.github.potjerodekool.openapi.common.generate.api.CodeGenerator
import java.lang.reflect.InvocationTargetException

class GeneratorRegistry {
    fun loadCodeGenerator(
        name: String,
        lang: String
    ): CodeGenerator {
        try {
            val resources = javaClass.classLoader.getResources(
                "io/github/potjerodekool/openapi/common/generate/api/CodeGenerator"
            )

            var generatorName: String?
            var generatorLang: String?
            var generatorClassName: String?

            while (resources.hasMoreElements()) {
                generatorName = null
                generatorLang = null
                generatorClassName = null
                val url = resources.nextElement()
                url.openStream().use { inputStream ->
                    val lines = String(inputStream.readAllBytes()).replace("\r", "")
                        .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (line in lines) {
                        val keyValue = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val key = keyValue[0]
                        val value = keyValue[1]

                        if (GENERATOR_NAME == key) {
                            generatorName = value
                        } else if (GENERATOR_LANG == key) {
                            generatorLang = value
                        } else if (GENERATOR_CLASS == key) {
                            generatorClassName = value
                        }
                    }
                    if (generatorName != null && generatorLang != null && generatorClassName != null) {
                        if (generatorName == name && generatorLang == lang) {
                            return createInstance(generatorClassName!!)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw GenerateException("Code generator not found")
        }

        throw GenerateException("Code generator not found")
    }

    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class,
        InvocationTargetException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun createInstance(className: String): CodeGenerator {
        val generatorClazz = javaClass.classLoader.loadClass(className) as Class<CodeGenerator>
        return generatorClazz.getDeclaredConstructor().newInstance()
    }

    companion object {
        const val GENERATOR_NAME: String = "generatorName"
        const val GENERATOR_LANG: String = "generatorLang"
        const val GENERATOR_CLASS: String = "generatorClass"
    }
}
