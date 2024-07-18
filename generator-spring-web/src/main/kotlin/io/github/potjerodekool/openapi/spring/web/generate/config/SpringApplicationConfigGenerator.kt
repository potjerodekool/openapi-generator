package io.github.potjerodekool.openapi.spring.web.generate.config

import io.github.potjerodekool.codegen.io.FileObject
import io.github.potjerodekool.codegen.io.Filer
import io.github.potjerodekool.codegen.io.Location
import io.github.potjerodekool.openapi.common.PropertiesUpdater.update
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.util.*

class SpringApplicationConfigGenerator(private val filer: Filer) {
    fun generate(additionalApplicationProperties: Map<String, Any>) {
        val fileOptional = resolveFile()
        fileOptional.ifPresent { file: FileObject ->
            if (file.kind == FileObject.Kind.PROPERTIES) {
                createOfUpdateApplicationProperties(
                    additionalApplicationProperties,
                    file
                )
            } else if (file.kind == FileObject.Kind.YAML) {
                createOfUpdateApplicationPropertiesYaml(
                    additionalApplicationProperties,
                    file
                )
            }
        }
    }

    private fun resolveFile(): Optional<FileObject> {
        val fileNames = listOf(
            "application.properties",
            "application.yml",
            "application.yaml",
            "application.properties"
        )

        return fileNames.stream()
            .map { fileName: String? -> filer.getResource(Location.RESOURCE_PATH, null, fileName) }
            .filter { obj: FileObject? -> Objects.nonNull(obj) }
            .findFirst()
    }

    private fun createOfUpdateApplicationProperties(
        additionalApplicationProperties: Map<String, Any>,
        fileObject: FileObject
    ) {
        if (!additionalApplicationProperties.isEmpty()) {
            update(fileObject, additionalApplicationProperties)
        }
    }

    private fun createOfUpdateApplicationPropertiesYaml(
        additionalApplicationProperties: Map<String, Any>,
        fileObject: FileObject
    ) {
        val yaml = Yaml()
        val yamlMap = HashMap<String, Any>()

        try {
            fileObject.openReader(false).use { reader ->
                val map = yaml.load<Map<String, Any>>(reader)
                yamlMap.putAll(map)
            }
        } catch (e: IOException) {
            //Ignore
        }

        var modified = false

        for ((key, value) in additionalApplicationProperties) {
            val keyElements = key.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (addValueToYaml(keyElements, 0, value, yamlMap)) {
                modified = true
            }
        }

        if (modified) {
            try {
                fileObject.openWriter().use { fileWriter ->
                    val code = yaml.dumpAsMap(yamlMap)
                    fileWriter.write(code)
                }
            } catch (e: IOException) {
                //Ignore
            }
        }
    }

    private fun addValueToYaml(
        keyElements: Array<String>,
        keyIndex: Int,
        value: Any,
        yamlMap: MutableMap<String, Any>?
    ): Boolean {
        val added: Boolean

        val lastKeyIndex = keyElements.size - 1
        val key = keyElements[keyIndex]

        if (yamlMap!!.containsKey(key)) {
            val subValue = yamlMap[key]

            if (keyIndex < lastKeyIndex) {
                val subMap = subValue as MutableMap<String, Any>?
                added = addValueToYaml(keyElements, keyIndex + 1, value, subMap)
            } else {
                added = false
            }
        } else if (keyIndex < lastKeyIndex) {
            val subMap = HashMap<String, Any>()
            yamlMap[key] = subMap
            added = addValueToYaml(keyElements, keyIndex + 1, value, subMap)
        } else {
            yamlMap[key] = value
            added = true
        }

        return added
    }
}

