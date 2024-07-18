package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema

interface OpenApiWalkerListener {

    fun visitOperation(
        api: OpenAPI,
        method: HttpMethod,
        path: String,
        operation: Operation?,
        apiConfiguration: ApiConfiguration
    ) {
    }

    fun visitContent(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?, content: Content?,
        apiConfiguration: ApiConfiguration
    ) {
    }

    fun visitSchema(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?,
        schema: Schema<*>?,
        contentType: ContentType?,
        schemaName: String?,
        apiConfiguration: ApiConfiguration
    ) {
    }
}
