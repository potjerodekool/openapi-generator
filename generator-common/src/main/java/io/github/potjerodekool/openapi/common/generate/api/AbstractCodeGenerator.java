package io.github.potjerodekool.openapi.common.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.common.generate.model.ModelsGenerator;
import io.github.potjerodekool.openapi.common.OpenApiEnvironment;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.Templates;
import io.github.potjerodekool.openapi.common.generate.service.ServiceApiGenerator;
import io.swagger.v3.oas.models.OpenAPI;

public abstract class AbstractCodeGenerator implements CodeGenerator {

    private final Templates templates = new Templates();


    @Override
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

    protected abstract OpenApiTypeUtils getTypeUtils();

    protected abstract void generateApiConfigs(final OpenAPI openApi,
                                               final OpenApiEnvironment openApiEnvironment);

}
