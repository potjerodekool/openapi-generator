package io.github.potjerodekool.openapi.common.generate.api

import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.OpenApiEnvironment
import io.swagger.v3.oas.models.OpenAPI

interface CodeGenerator {
    fun generateCommon(openApiEnvironment: OpenApiEnvironment)

    fun generateApi(
        openApiEnvironment: OpenApiEnvironment,
        openApi: OpenAPI,
        apiConfiguration: ApiConfiguration,
        generateConfigs: Boolean
    )
}
