package io.github.potjerodekool.openapi.common

import java.io.File

data class ApiConfiguration(
    val apiFile: File,
    val schemasDir: File,
    val pathsDir: File,
    val basePackageName: String,
    val isGenerateApiDefinitions: Boolean,
    val generateApiImplementations: Boolean,
    val generateModels: Boolean
) {
    constructor(
        apiFile: File,
        basePackageName: String,
        generateApiDefinitions: Boolean,
        generateApiImplementations: Boolean,
        generateModels: Boolean
    ) : this(
        apiFile,
        createRelativeFile(apiFile, "schemas"),
        createRelativeFile(apiFile, "paths"),
        basePackageName,
        generateApiDefinitions,
        generateApiImplementations,
        generateModels
    )

    fun modelPackageName(): String {
        return "$basePackageName.model"
    }

    companion object {
        private fun createRelativeFile(
            file: File,
            name: String
        ): File {
            return File(file.parentFile, name)
        }
    }
}

