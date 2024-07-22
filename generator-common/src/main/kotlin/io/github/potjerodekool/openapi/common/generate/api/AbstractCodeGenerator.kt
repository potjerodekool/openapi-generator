package io.github.potjerodekool.openapi.common.generate.api

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.OpenApiEnvironment
import io.github.potjerodekool.openapi.common.generate.StandardOpenApiTypeUtils
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils
import io.github.potjerodekool.openapi.common.generate.Templates
import io.github.potjerodekool.openapi.common.generate.config.ConfigGenerator
import io.github.potjerodekool.openapi.common.generate.model.ModelsGenerator
import io.github.potjerodekool.openapi.common.generate.service.ServiceApiGenerator
import io.swagger.v3.oas.models.OpenAPI
import java.util.function.Consumer

abstract class AbstractCodeGenerator : CodeGenerator {
    private val templates = Templates()


    override fun generateCommon(openApiEnvironment: OpenApiEnvironment) {
        generateUtils(openApiEnvironment)
        generateConfigs(openApiEnvironment)
    }

    private fun generateUtils(openApiEnvironment: OpenApiEnvironment) {
        val generatorConfig = openApiEnvironment.generatorConfig
        val environment = openApiEnvironment.environment

        generateApiUtils(generatorConfig, environment)

        RequestGenerator(generatorConfig, environment, this.templates).generate()
        HttpServletRequestWrapperGenerator(
            generatorConfig,
            environment,
            templates
        ).generate()
    }

    private fun generateApiUtils(
        generatorConfig: GeneratorConfig,
        environment: Environment
    ) {
        UtilsGenerator(generatorConfig, environment, this.templates)
            .generate()
    }


    protected open fun generateConfigs(openApiEnvironment: OpenApiEnvironment) {
        val applicationContext = openApiEnvironment.applicationContext
        val configGenerators = applicationContext.getBeansOfType(
            ConfigGenerator::class.java
        )
        configGenerators.forEach(Consumer { obj: ConfigGenerator -> obj.generate() })
    }

    override fun generateApi(
        openApiEnvironment: OpenApiEnvironment,
        openApi: OpenAPI,
        apiConfiguration: ApiConfiguration,
        generateConfigs: Boolean
    ) {
        generateModels(openApiEnvironment, openApi, apiConfiguration)
        generateApiDefinition(openApi, apiConfiguration, openApiEnvironment)
        generateServiceDefinition(openApi, openApiEnvironment, apiConfiguration)
        generateApiImplementation(openApi, apiConfiguration, openApiEnvironment)

        if (generateConfigs) {
            generateApiConfigs(openApi, openApiEnvironment)
        }
    }

    private fun generateModels(
        openApiEnvironment: OpenApiEnvironment,
        openApi: OpenAPI,
        apiConfiguration: ApiConfiguration
    ) {
        val generateModels = apiConfiguration.generateModels

        if (generateModels) {
            val generator = ModelsGenerator(
                templates,
                apiConfiguration.modelPackageName(),
                openApiEnvironment,
                typeUtils
            )
            generator.generateModels(openApi, apiConfiguration)
        }
    }

    protected abstract fun generateApiDefinition(
        openApi: OpenAPI,
        apiConfiguration: ApiConfiguration?,
        openApiEnvironment: OpenApiEnvironment?
    )

    protected abstract fun generateApiImplementation(
        openAPI: OpenAPI,
        apiConfiguration: ApiConfiguration?,
        openApiEnvironment: OpenApiEnvironment?
    )

    private fun generateServiceDefinition(
        openApi: OpenAPI,
        openApiEnvironment: OpenApiEnvironment,
        apiConfiguration: ApiConfiguration
    ) {
        if (apiConfiguration.generateApiImplementations) {
            ServiceApiGenerator(
                openApiEnvironment.generatorConfig,
                apiConfiguration,
                openApiEnvironment.environment,
                typeUtils
            )
                .generate(openApi)
        }
    }

    protected open val typeUtils: OpenApiTypeUtils?
        get() = StandardOpenApiTypeUtils()

    protected abstract fun generateApiConfigs(
        openApi: OpenAPI,
        openApiEnvironment: OpenApiEnvironment?
    )
}
