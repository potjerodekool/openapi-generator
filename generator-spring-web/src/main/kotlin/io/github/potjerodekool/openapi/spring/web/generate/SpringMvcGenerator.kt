package io.github.potjerodekool.openapi.spring.web.generate

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.OpenApiEnvironment
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils
import io.github.potjerodekool.openapi.common.generate.api.AbstractCodeGenerator
import io.github.potjerodekool.openapi.common.generate.config.ApiConfigGenerator
import io.github.potjerodekool.openapi.spring.web.generate.api.SpringApiGenerator
import io.github.potjerodekool.openapi.spring.web.generate.api.SpringRestControllerGenerator
import io.github.potjerodekool.openapi.spring.web.generate.config.SpringApplicationConfigGenerator
import io.github.potjerodekool.openapi.spring.web.generate.config.SpringJacksonConfigGenerator
import io.github.potjerodekool.openapi.spring.web.generate.config.SpringOpenApiConfigGenerator
import io.swagger.v3.oas.models.OpenAPI
import java.util.function.Consumer

class SpringMvcGenerator : AbstractCodeGenerator() {
    private val additionalApplicationProperties: Map<String, Any> = HashMap()

    override fun generateConfigs(openApiEnvironment: OpenApiEnvironment) {
        super.generateConfigs(openApiEnvironment)
        val generatorConfig = openApiEnvironment.generatorConfig
        val environment = openApiEnvironment.environment
        val dependencyChecker = openApiEnvironment.project.dependencyChecker
        SpringJacksonConfigGenerator(generatorConfig, environment, dependencyChecker).generate()
    }

    private fun generateSpringConfig(
        additionalApplicationProperties: Map<String, Any>,
        environment: Environment
    ) {
        SpringApplicationConfigGenerator(environment.filer)
            .generate(additionalApplicationProperties)
    }

    override fun generateApiConfigs(
        openApi: OpenAPI,
        openApiEnvironment: OpenApiEnvironment?
    ) {
        val generatorConfig = openApiEnvironment!!.generatorConfig
        val environment = openApiEnvironment.environment
        val applicationContext = openApiEnvironment.applicationContext

        SpringOpenApiConfigGenerator(generatorConfig, environment).generate(openApi)

        val configGenerators = applicationContext.getBeansOfType(
            ApiConfigGenerator::class.java
        )
        configGenerators.forEach(Consumer { configGenerator: ApiConfigGenerator -> configGenerator.generate(openApi) })
    }

    override fun generateApiDefinition(
        openApi: OpenAPI,
        apiConfiguration: ApiConfiguration?,
        openApiEnvironment: OpenApiEnvironment?
    ) {
        val generatorConfig = openApiEnvironment!!.generatorConfig
        val environment = openApiEnvironment.environment

        SpringApiGenerator(
            generatorConfig,
            apiConfiguration,
            typeUtils,
            environment
        ).generate(openApi!!)
    }

    override fun generateApiImplementation(
        openAPI: OpenAPI,
        apiConfiguration: ApiConfiguration?,
        openApiEnvironment: OpenApiEnvironment?
    ) {
        if (apiConfiguration!!.generateApiImplementations) {
            val generatorConfig = openApiEnvironment!!.generatorConfig
            val environment = openApiEnvironment.environment
            val generator = SpringRestControllerGenerator(
                generatorConfig,
                apiConfiguration,
                typeUtils,
                environment
            )

            generator.generate(openAPI)
        }
    }

    override fun generateCommon(openApiEnvironment: OpenApiEnvironment) {
        super.generateCommon(openApiEnvironment)
        val environment = openApiEnvironment.environment
        generateSpringConfig(additionalApplicationProperties, environment)
    }

    override val typeUtils: OpenApiTypeUtils
        get() = TypeUtilsSpringImpl()
}
