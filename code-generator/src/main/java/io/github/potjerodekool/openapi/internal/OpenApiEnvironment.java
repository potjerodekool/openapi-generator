package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.Project;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;

public class OpenApiEnvironment {

    private final Project project;
    private final OpenApiTypeUtils openApiTypeUtils;
    private final Environment environment;
    private final GeneratorConfig generatorConfig;
    private final ApplicationContext applicationContext;

    public OpenApiEnvironment(final Project project,
                              final OpenApiTypeUtils openApiTypeUtils,
                              final Environment environment,
                              final GeneratorConfig generatorConfig,
                              final ApplicationContext applicationContext) {
        this.project = project;
        this.openApiTypeUtils = openApiTypeUtils;
        this.environment = environment;
        this.generatorConfig = generatorConfig;
        this.applicationContext = applicationContext;
    }

    public Project getProject() {
        return project;
    }

    public OpenApiTypeUtils getOpenApiTypeUtils() {
        return openApiTypeUtils;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public GeneratorConfig getGeneratorConfig() {
        return generatorConfig;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
