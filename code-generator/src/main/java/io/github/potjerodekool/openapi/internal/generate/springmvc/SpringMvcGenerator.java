package io.github.potjerodekool.openapi.internal.generate.springmvc;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.OpenApiEnvironment;
import io.github.potjerodekool.openapi.internal.generate.api.AbstractCodeGenerator;
import io.github.potjerodekool.openapi.internal.generate.incurbation.TypeUtils;
import io.github.potjerodekool.openapi.internal.generate.incurbation.TypeUtilsSpringImpl;
import io.github.potjerodekool.openapi.internal.generate.springmvc.api.SpringApiGenerator;
import io.github.potjerodekool.openapi.internal.generate.springmvc.api.SpringRestControllerGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringApplicationConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.internal.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtilsJava;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.HashMap;
import java.util.Map;

public class SpringMvcGenerator extends AbstractCodeGenerator {

    private final Map<String, Object> additionalApplicationProperties = new HashMap<>();

    @Override
    protected void generateConfigs(final OpenApiEnvironment openApiEnvironment) {
        super.generateConfigs(openApiEnvironment);

        timedAction(() -> {
            final var generatorConfig = openApiEnvironment.getGeneratorConfig();
            final var environment = openApiEnvironment.getEnvironment();
            final var dependencyChecker = openApiEnvironment.getProject().dependencyChecker();
            new SpringJacksonConfigGenerator(generatorConfig, environment, dependencyChecker).generate();
        }, "SpringMvcGenerator.generateConfigs");
    }

    private void generateSpringConfig(final Map<String, Object> additionalApplicationProperties,
                                      final Environment environment) {
        new SpringApplicationConfigGenerator(environment.getFiler())
                .generate(additionalApplicationProperties);
    }

    @Override
    protected void generateApiConfigs(final OpenAPI openApi,
                                      final OpenApiEnvironment openApiEnvironment) {
        timedAction(() -> {
            final var generatorConfig = openApiEnvironment.getGeneratorConfig();
            final var environment = openApiEnvironment.getEnvironment();
            final var applicationContext = openApiEnvironment.getApplicationContext();

            new SpringOpenApiConfigGenerator(generatorConfig, environment).generate(openApi);

            final var configGenerators = applicationContext.getBeansOfType(ApiConfigGenerator.class);
            configGenerators.forEach(configGenerator -> configGenerator.generate(openApi));
        }, "SpringMvcGenerator.generateApiConfigs");

    }

    @Override
    protected void generateApiDefinition(final OpenAPI openApi,
                                         final ApiConfiguration apiConfiguration,
                                         final OpenApiEnvironment openApiEnvironment) {
        timedAction(() -> {
            final var generatorConfig = openApiEnvironment.getGeneratorConfig();
            final var environment = openApiEnvironment.getEnvironment();
            final var openApiTypeUtils = getOpenApiTypeUtils(apiConfiguration);
            new SpringApiGenerator(generatorConfig, apiConfiguration, openApiTypeUtils, environment).generate(openApi);
        }, "SpringMvcGenerator.generateApiDefinition");
    }

    @Override
    protected void generateApiImplementation(final OpenAPI openApi,
                                             final ApiConfiguration apiConfiguration,
                                             final OpenApiEnvironment openApiEnvironment) {
        timedAction(() -> {
            if (apiConfiguration.generateApiImplementations()) {
                final var generatorConfig = openApiEnvironment.getGeneratorConfig();
                final var environment = openApiEnvironment.getEnvironment();
                final var openApiTypeUtils = getOpenApiTypeUtils(apiConfiguration);

                final var generator = new SpringRestControllerGenerator(
                        generatorConfig,
                        apiConfiguration,
                        environment,
                        openApiTypeUtils
                );

                generator.generate(openApi);
            }
        }, "SpringMvcGenerator.generateApiImplementation");
    }

    @Override
    public void generate(final Environment environment) {
        generateSpringConfig(additionalApplicationProperties, environment);
    }

    @Override
    public OpenApiTypeUtils getOpenApiTypeUtils(final ApiConfiguration apiConfiguration) {
        return new OpenApiTypeUtilsSpringImpl(new OpenApiTypeUtilsJava(apiConfiguration.modelPackageName()));
    }

    @Override
    protected TypeUtils getTypeUtils() {
        return new TypeUtilsSpringImpl();
    }
}
