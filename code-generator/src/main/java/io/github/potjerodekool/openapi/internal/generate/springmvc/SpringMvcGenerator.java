package io.github.potjerodekool.openapi.internal.generate.springmvc;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.OpenApiEnvironment;
import io.github.potjerodekool.openapi.internal.generate.api.AbstractCodeGenerator;
import io.github.potjerodekool.openapi.internal.generate.springmvc.api.SpringApiGenerator;
import io.github.potjerodekool.openapi.internal.generate.springmvc.api.SpringRestControllerGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringApplicationConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.util.HashMap;
import java.util.Map;

public class SpringMvcGenerator extends AbstractCodeGenerator {

    private final OpenApiTypeUtils openApiTypeUtils = new OpenApiTypeUtilsSpringImpl();

    private final Map<String, Object> additionalApplicationProperties = new HashMap<>();

    @Override
    protected void generateConfigs(final OpenApiEnvironment openApiEnvironment) {
        super.generateConfigs(openApiEnvironment);

        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var environment = openApiEnvironment.getEnvironment();
        final var dependencyChecker = openApiEnvironment.getProject().dependencyChecker();
        new SpringJacksonConfigGenerator(generatorConfig, environment, dependencyChecker).generate();
    }

    private void generateSpringConfig(final Map<String, Object> additionalApplicationProperties,
                                      final Environment environment) {
        new SpringApplicationConfigGenerator(environment.getFiler())
                .generate(additionalApplicationProperties);
    }

    @Override
    protected void generateApiConfigs(final OpenApi api,
                                      final OpenApiEnvironment openApiEnvironment) {
        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var environment = openApiEnvironment.getEnvironment();
        final var applicationContext = openApiEnvironment.getApplicationContext();

        new SpringOpenApiConfigGenerator(generatorConfig, environment).generate(api);

        final var configGenerators = applicationContext.getBeansOfType(ApiConfigGenerator.class);
        configGenerators.forEach(configGenerator -> configGenerator.generate(api));
    }

    @Override
    protected void generateApiDefinition(final OpenApi api,
                                         final ApiConfiguration apiConfiguration,
                                         final OpenApiEnvironment openApiEnvironment) {
        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var openApiTypeUtils = openApiEnvironment.getOpenApiTypeUtils();
        final var environment = openApiEnvironment.getEnvironment();

        new SpringApiGenerator(generatorConfig, apiConfiguration, openApiTypeUtils, environment).generate(api);
    }

    @Override
    protected void generateApiImplementation(final OpenApi api,
                                             final ApiConfiguration apiConfiguration,
                                             final OpenApiEnvironment openApiEnvironment) {
        if (apiConfiguration.generateApiImplementations()) {
            final var generatorConfig = openApiEnvironment.getGeneratorConfig();
            final var openApiTypeUtils = openApiEnvironment.getOpenApiTypeUtils();
            final var environment = openApiEnvironment.getEnvironment();

            final var generator = new SpringRestControllerGenerator(
                    generatorConfig,
                    apiConfiguration,
                    environment,
                    openApiTypeUtils
            );

            generator.generate(api);
        }
    }

    @Override
    public void generate(final Environment environment) {
        generateSpringConfig(additionalApplicationProperties, environment);
    }

    @Override
    public OpenApiTypeUtils getOpenApiTypeUtils() {
        return openApiTypeUtils;
    }
}
