package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.OpenApiEnvironment;
import io.github.potjerodekool.openapi.internal.generate.Templates;
import io.github.potjerodekool.openapi.internal.generate.incurbation.TypeUtils;
import io.github.potjerodekool.openapi.internal.generate.incurbation.service.ServiceApiGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.ModelsGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.swagger.v3.oas.models.OpenAPI;

public abstract class AbstractCodeGenerator {

    private final Templates templates = new Templates();


    public void generateCommon(final OpenApiEnvironment openApiEnvironment) {
        generateUtils(openApiEnvironment);
        generateConfigs(openApiEnvironment);
    }

    private void generateUtils(final OpenApiEnvironment openApiEnvironment) {
        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var environment = openApiEnvironment.getEnvironment();

        generateApiUtils(generatorConfig, environment);

        new RequestGenerator(generatorConfig, environment, this.templates).generate();
        new HttpServletRequestWrapperGenerator(
                generatorConfig,
                environment,
                templates
        ).generate();
    }

    private void generateApiUtils(final GeneratorConfig generatorConfig,
                                  final Environment environment) {
        new UtilsGenerator(generatorConfig, environment, this.templates)
                .generate();
    }


    protected void generateConfigs(final OpenApiEnvironment openApiEnvironment) {
        final var applicationContext = openApiEnvironment.getApplicationContext();
        final var configGenerators = applicationContext.getBeansOfType(ConfigGenerator.class);
        configGenerators.forEach(ConfigGenerator::generate);
    }

    public void generateApi(final OpenApiEnvironment openApiEnvironment,
                            final OpenAPI openApi,
                            final ApiConfiguration apiConfiguration,
                            final boolean generateConfigs) {
        generateModels(openApiEnvironment, openApi, apiConfiguration);
        generateApiDefinition(openApi, apiConfiguration, openApiEnvironment);
        generateServiceDefinition(openApi, openApiEnvironment, apiConfiguration);
        generateApiImplementation(openApi, apiConfiguration, openApiEnvironment);

        if (generateConfigs) {
            generateApiConfigs(openApi, openApiEnvironment);
        }
    }

    private void generateModels(final OpenApiEnvironment openApiEnvironment,
                                final OpenAPI openApi,
                                final ApiConfiguration apiConfiguration) {
        final var generateModels = apiConfiguration.generateModels();

        if (generateModels) {
            final var generator = new ModelsGenerator(
                    templates,
                    apiConfiguration.modelPackageName(),
                    openApiEnvironment
            );
            generator.generateModels(openApi);
        }
    }

    protected abstract void generateApiDefinition(final OpenAPI openApi,
                                                  final ApiConfiguration apiConfiguration,
                                                  final OpenApiEnvironment openApiEnvironment);

    protected abstract void generateApiImplementation(final OpenAPI openAPI,
                                                      final ApiConfiguration apiConfiguration,
                                                      final OpenApiEnvironment openApiEnvironment);

    protected void generateServiceDefinition(final OpenAPI openApi,
                                             final OpenApiEnvironment openApiEnvironment,
                                             final ApiConfiguration apiConfiguration) {
        if (apiConfiguration.generateApiImplementations()) {
            new ServiceApiGenerator(
                    openApiEnvironment.getGeneratorConfig(),
                    apiConfiguration,
                    openApiEnvironment.getEnvironment(),
                    getTypeUtils())
                    .generate(openApi);
        }
    }

    protected abstract TypeUtils getTypeUtils();

    protected abstract void generateApiConfigs(final OpenAPI openApi,
                                               final OpenApiEnvironment openApiEnvironment);

    public void generate(final Environment environment) {
    }

    public abstract OpenApiTypeUtils getOpenApiTypeUtils(final ApiConfiguration apiConfiguration);
}
