package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.getMediaType
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse

class OpenApiWalker(
    private val api: OpenAPI,
    private val apiConfiguration: ApiConfiguration
) {
    fun walk(listener: OpenApiWalkerListener) {
        api.paths
            .forEach { path: String, pathItem: PathItem ->
                visitPath(
                    api, path, pathItem, listener
                )
            }
    }

    private fun visitPath(
        openAPI: OpenAPI,
        path: String,
        pathItem: PathItem,
        listener: OpenApiWalkerListener
    ) {
        visitOperation(openAPI, path, HttpMethod.POST, pathItem.post, listener)
        visitOperation(openAPI, path, HttpMethod.GET, pathItem.get, listener)
        visitOperation(openAPI, path, HttpMethod.PUT, pathItem.put, listener)
        visitOperation(openAPI, path, HttpMethod.DELETE, pathItem.delete, listener)
        visitOperation(openAPI, path, HttpMethod.PATCH, pathItem.patch, listener)
        visitOperation(openAPI, path, HttpMethod.OPTIONS, pathItem.options, listener)
        visitOperation(openAPI, path, HttpMethod.HEAD, pathItem.head, listener)
    }

    private fun visitOperation(
        openAPI: OpenAPI,
        path: String,
        httpMethod: HttpMethod,
        operation: Operation?,
        listener: OpenApiWalkerListener
    ) {
        if (operation != null) {
            listener.visitOperation(openAPI, httpMethod, path, operation, apiConfiguration)
            val requestBody = operation.requestBody

            if (requestBody != null) {
                val content = requestBody.content
                visitContent(openAPI, httpMethod, path, operation, content, listener)
            }

            val responses = operation.responses

            responses?.forEach { responseCode: String?, response: ApiResponse ->
                visitContent(
                    openAPI,
                    httpMethod,
                    path,
                    operation,
                    response.content,
                    listener
                )
            }
        }
    }

    private fun visitContent(
        openAPI: OpenAPI,
        httpMethod: HttpMethod,
        path: String,
        operation: Operation,
        content: Content?,
        listener: OpenApiWalkerListener
    ) {
        if (content != null) {
            listener.visitContent(
                openAPI,
                httpMethod,
                path,
                operation,
                content,
                apiConfiguration
            )

            val mt = getMediaType(content)

            if (mt != null) {
                val mediaType = mt.value
                val contentType = mt.key
                val schema = mediaType.schema

                if (schema != null) {
                    visitSchema(openAPI, httpMethod, path, operation, schema, contentType, listener)
                }
            }
        }
    }

    private fun visitSchema(
        openAPI: OpenAPI,
        httpMethod: HttpMethod,
        path: String,
        operation: Operation,
        schema: Schema<*>,
        contentType: ContentType,
        listener: OpenApiWalkerListener
    ) {
        listener.visitSchema(openAPI, httpMethod, path, operation, schema, contentType, null, apiConfiguration)
    }
}
