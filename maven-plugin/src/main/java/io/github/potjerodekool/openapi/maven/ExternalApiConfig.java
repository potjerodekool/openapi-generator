package io.github.potjerodekool.openapi.maven;

import org.apache.maven.plugins.annotations.Parameter;

public class ExternalApiConfig {

    @Parameter(property = "openApiFile")
    private String openApiFile;

    @Parameter(property = "modelPackageName")
    private String modelPackageName;

    public String getOpenApiFile() {
        return openApiFile;
    }

    public String getModelPackageName() {
        return modelPackageName;
    }
}
