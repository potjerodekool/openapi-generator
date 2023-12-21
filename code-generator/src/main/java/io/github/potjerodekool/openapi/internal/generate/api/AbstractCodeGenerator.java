package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.OpenApiEnvironment;
import io.github.potjerodekool.openapi.internal.generate.model.ModelsCodeGenerator;
import io.github.potjerodekool.openapi.internal.generate.service.ServiceApiGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApi;

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
        //new ApiUtilsGenerator(generatorConfig, environment).generate();
    }


    protected void generateConfigs(final OpenApiEnvironment openApiEnvironment) {
        final var applicationContext = openApiEnvironment.getApplicationContext();
        final var configGenerators = applicationContext.getBeansOfType(ConfigGenerator.class);
        configGenerators.forEach(ConfigGenerator::generate);
    }

    public void generateApi(final OpenApiEnvironment openApiEnvironment,
                            final OpenApi api,
                            final ApiConfiguration apiConfiguration,
                            final boolean generateConfigs) {
        generateModels(openApiEnvironment, api, apiConfiguration);
        generateApiDefinition(api, apiConfiguration, openApiEnvironment);
        generateServiceDefinition(api, openApiEnvironment, apiConfiguration);
        generateApiImplementation(api, apiConfiguration, openApiEnvironment);

        if (generateConfigs) {
            generateApiConfigs(api, openApiEnvironment);
        }
    }

    private void generateModels(final OpenApiEnvironment openApiEnvironment,
                                final OpenApi api,
                                final ApiConfiguration apiConfiguration) {
        final var generateModels = apiConfiguration.generateModels();

        if (generateModels) {
            new ModelsCodeGenerator(openApiEnvironment).generate(api);
        }
    }

    protected abstract void generateApiDefinition(final OpenApi api,
                                                  final ApiConfiguration apiConfiguration,
                                                  final OpenApiEnvironment openApiEnvironment);

    protected abstract void generateApiImplementation(final OpenApi api,
                                                      final ApiConfiguration apiConfiguration,
                                                      final OpenApiEnvironment openApiEnvironment);

    protected void generateServiceDefinition(final OpenApi api,
                                             final OpenApiEnvironment openApiEnvironment,
                                             final ApiConfiguration apiConfiguration) {
        if (apiConfiguration.generateApiImplementations()) {
            final var generator = new ServiceApiGenerator(
                    openApiEnvironment.getGeneratorConfig(),
                    apiConfiguration,
                    openApiEnvironment.getEnvironment(),
                    openApiEnvironment.getOpenApiTypeUtils()
            );
            generator.generate(api);
        }
    }

    protected abstract void generateApiConfigs(final OpenApi api,
                                               final OpenApiEnvironment openApiEnvironment);

    public void generate(final Environment environment) {
    }

    public abstract OpenApiTypeUtils getOpenApiTypeUtils();
}
