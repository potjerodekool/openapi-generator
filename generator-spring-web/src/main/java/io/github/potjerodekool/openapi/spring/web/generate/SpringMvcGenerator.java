package io.github.potjerodekool.openapi.spring.web.generate;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.common.OpenApiEnvironment;
import io.github.potjerodekool.openapi.common.generate.api.AbstractCodeGenerator;
import io.github.potjerodekool.openapi.spring.web.generate.config.SpringApplicationConfigGenerator;
import io.github.potjerodekool.openapi.spring.web.generate.config.SpringJacksonConfigGenerator;
import io.github.potjerodekool.openapi.spring.web.generate.config.SpringOpenApiConfigGenerator;
import io.github.potjerodekool.openapi.spring.web.generate.api.SpringApiGenerator;
import io.github.potjerodekool.openapi.spring.web.generate.api.SpringRestControllerGenerator;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.HashMap;
import java.util.Map;

public class SpringMvcGenerator extends AbstractCodeGenerator {

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
    protected void generateApiConfigs(final OpenAPI openApi,
                                      final OpenApiEnvironment openApiEnvironment) {
        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var environment = openApiEnvironment.getEnvironment();
        final var applicationContext = openApiEnvironment.getApplicationContext();

        new SpringOpenApiConfigGenerator(generatorConfig, environment).generate(openApi);

        final var configGenerators = applicationContext.getBeansOfType(ApiConfigGenerator.class);
        configGenerators.forEach(configGenerator -> configGenerator.generate(openApi));
    }

    @Override
    protected void generateApiDefinition(final OpenAPI openApi,
                                         final ApiConfiguration apiConfiguration,
                                         final OpenApiEnvironment openApiEnvironment) {
        final var generatorConfig = openApiEnvironment.getGeneratorConfig();
        final var environment = openApiEnvironment.getEnvironment();

        new SpringApiGenerator(
                generatorConfig,
                apiConfiguration,
                getTypeUtils(),
                environment
        ).generate(openApi);
    }

    @Override
    protected void generateApiImplementation(final OpenAPI openApi,
                                             final ApiConfiguration apiConfiguration,
                                             final OpenApiEnvironment openApiEnvironment) {
        if (apiConfiguration.generateApiImplementations()) {
            final var generatorConfig = openApiEnvironment.getGeneratorConfig();
            final var environment = openApiEnvironment.getEnvironment();
            final var generator = new SpringRestControllerGenerator(
                    generatorConfig,
                    apiConfiguration,
                    getTypeUtils(),
                    environment
            );

            generator.generate(openApi);
        }
    }

    @Override
    public void generateCommon(final OpenApiEnvironment openApiEnvironment) {
        super.generateCommon(openApiEnvironment);
        final var environment = openApiEnvironment.getEnvironment();
        generateSpringConfig(additionalApplicationProperties, environment);
    }

    @Override
    protected OpenApiTypeUtils getTypeUtils() {
        return new TypeUtilsSpringImpl(super.getTypeUtils());
    }
}
