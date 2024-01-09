package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.OpenApiEnvironment;
import io.github.potjerodekool.openapi.internal.generate.model.ModelsGenerator;
import io.github.potjerodekool.openapi.internal.generate.model.OpenApiWalker;
import io.github.potjerodekool.openapi.internal.generate.model.adapt.ModelAdapter;
import io.github.potjerodekool.openapi.internal.generate.service.ServiceApiGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.swagger.v3.oas.models.OpenAPI;

public abstract class AbstractCodeGenerator {

    public void generateCommon(final OpenApiEnvironment openApiEnvironment) {
        generateUtils(openApiEnvironment);
        generateConfigs(openApiEnvironment);
    }

    private void generateUtils(final OpenApiEnvironment openApiEnvironment) {
        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var environment = openApiEnvironment.getEnvironment();

        generateApiUtils(generatorConfig, environment);

        new RequestGenerator(generatorConfig, environment).generate();
        new HttpServletRequestWrapperGenerator(generatorConfig, environment).generate();
    }

    private void generateApiUtils(final GeneratorConfig generatorConfig,
                                  final Environment environment) {
        new UtilsGenerator(generatorConfig, environment).generate();
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
            final var applicationContext = openApiEnvironment.getApplicationContext();
            final var adapters = applicationContext.getBeansOfType(ModelAdapter.class);

            final var generator = new ModelsGenerator(apiConfiguration.modelPackageName());
            adapters.forEach(generator::registerModelAdapter);

            OpenApiWalker.walk(openApi, generator);
            final var models = generator.getModels();
            openApiEnvironment.getEnvironment().getCompilationUnits().addAll(models);
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
            final var generator = new ServiceApiGenerator(
                    openApiEnvironment.getGeneratorConfig(),
                    apiConfiguration,
                    openApiEnvironment.getEnvironment(),
                    getOpenApiTypeUtils(apiConfiguration)
            );
            generator.generate(openApi);
        }
    }

    protected abstract void generateApiConfigs(final OpenAPI openApi,
                                               final OpenApiEnvironment openApiEnvironment);

    public void generate(final Environment environment) {
    }

    public abstract OpenApiTypeUtils getOpenApiTypeUtils(final ApiConfiguration apiConfiguration);
}
