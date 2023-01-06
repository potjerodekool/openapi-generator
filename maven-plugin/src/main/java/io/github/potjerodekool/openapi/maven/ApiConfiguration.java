package io.github.potjerodekool.openapi.maven;

import org.apache.maven.plugins.annotations.Parameter;

public class ApiConfiguration {

    @Parameter(property = "openApiFile")
    private String openApiFile;

    @Parameter(property = "basePackageName")
    private String basePackageName;

    @Parameter(property = "generateModels", defaultValue = "true")
    private boolean generateModels = true;

    @Parameter(property = "generateApiDefinitions", defaultValue = "false")
    private boolean generateApiDefinitions = false;

    public String getOpenApiFile() {
        return openApiFile;
    }

    public void setOpenApiFile(final String openApiFile) {
        this.openApiFile = openApiFile;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(final String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public boolean isGenerateModels() {
        return generateModels;
    }

    public void setGenerateModels(final boolean generateModels) {
        this.generateModels = generateModels;
    }

    public boolean isGenerateApiDefinitions() {
        return generateApiDefinitions;
    }

    public void setGenerateApiDefinitions(final boolean generateApiDefinitions) {
        this.generateApiDefinitions = generateApiDefinitions;
    }

}
