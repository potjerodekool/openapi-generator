package io.github.potjerodekool.openapi.common.dependency

import java.io.File

data class Artifact(
    val groupId: String,
    val artifactId: String,
    val file: File?,
    val classifier: String?,
    val type: String
)
