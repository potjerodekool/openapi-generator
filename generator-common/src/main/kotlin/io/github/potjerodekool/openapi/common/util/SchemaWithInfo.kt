package io.github.potjerodekool.openapi.common.util

import io.github.potjerodekool.openapi.common.generate.ContentType
import io.swagger.v3.oas.models.media.Schema

data class SchemaWithInfo(
    val schema: Schema<*>?,
    val extensions: Map<String, Any>,
    val mediaType: ContentType
)
