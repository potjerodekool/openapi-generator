package io.github.potjerodekool.openapi.common.generate.model.adapter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.generate.ExtensionsHelper.getExtension
import io.github.potjerodekool.openapi.common.generate.annotation.TypeInfo
import io.github.potjerodekool.openapi.common.generate.model.element.Model
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import java.io.File
import java.io.IOException
import java.util.function.Consumer

class CustomAnnotationsModelAdapter : AbstractModelAdapter() {
    private val apiConfigurations: MutableMap<String, MutableMap<String, Any?>> = HashMap()

    private fun resolveConfigMap(apiConfiguration: ApiConfiguration): MutableMap<String, Any?> {
        val apiFile = apiConfiguration.apiFile
        val path = apiFile.absolutePath

        return apiConfigurations.computeIfAbsent(path) { key: String? ->
            var name = apiFile.name
            val sep = name.lastIndexOf(".")
            name = name.substring(0, sep) + "-config.json"
            val configFile = File(apiFile.parentFile, name)
            readConfigFile(configFile)
        }
    }

    private fun readConfigFile(configFile: File): MutableMap<String, Any?> {
        if (configFile.exists()) {
            try {
                return ObjectMapper().readValue(configFile, object: TypeReference<MutableMap<String, Any?>>(){})
            } catch (ignored: IOException) {
                // ignore
            }
        }

        return java.util.Map.of()
    }

    override fun adaptProperty(
        modelProperty: ModelProperty,
        schema: ObjectSchema,
        apiConfiguration: ApiConfiguration
    ) {
        findPropertySchema(schema, modelProperty.simpleName).ifPresent { propertySchema: Schema<*> ->
            val configMap = resolveConfigMap(apiConfiguration)
            val extensions = resolveExtensions(propertySchema, modelProperty, configMap)

            val annotationNames = getExtension(
                extensions,
                "x-annotations",
                object : TypeInfo<List<String>?>() {
                }
            )

            if (annotationNames != null) {
                annotationNames.forEach(Consumer { annotationName: String? ->
                    modelProperty.annotation(
                        Annot(
                            annotationName
                        )
                    )
                })
            }
        }
    }

    private fun resolveExtensions(
        schema: Schema<*>,
        modelProperty: ModelProperty,
        configMap: MutableMap<String, Any?>
    ): MutableMap<String, Any?>{
        if (schema.extensions != null) {
            return schema.extensions
        } else {
            val model = modelProperty.enclosedElement as Model
            val modelName = model.simpleName
            val schemaConfig = resolveSchemaConfig(modelName, configMap)
            val propertiesConfig =
                schemaConfig.getOrDefault("properties", mutableMapOf<String, Any?>()) as MutableMap<String, Any?>
            val map = propertiesConfig.getOrDefault(
                modelProperty.simpleName,
                mutableMapOf<String, Any?>()
            ) as MutableMap<String, Any?>

            val extensionsMap = mutableMapOf<String, Any?>()

            map.entries.stream()
                .filter { entry: MutableMap.MutableEntry<String, Any?> -> entry.key.startsWith("x-") }
                .map { entry: MutableMap.MutableEntry<String, Any?> -> java.util.Map.entry(entry.key, entry.value) }
                .forEach { entry: MutableMap.MutableEntry<String, Any?> -> extensionsMap[entry.key] = entry.value }

            return extensionsMap
        }
    }

    private fun resolveSchemaConfig(
        name: String,
        configMap: MutableMap<String, Any?>
    ): MutableMap<String, Any?> {
        val keys = "components.schemas.$name".split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return resolveSubMap(keys, 0, configMap)
    }

    private fun resolveSubMap(
        keys: Array<String>,
        index: Int,
        map: MutableMap<String, Any?>
    ): MutableMap<String, Any?> {
        val key = keys[index]
        val subMap = map.getOrDefault(key, mutableMapOf<Any, Any>()) as MutableMap<String, Any?>

        return if (index < keys.size - 1) {
            resolveSubMap(keys, index + 1, subMap)
        } else {
            subMap
        }
    }
}
