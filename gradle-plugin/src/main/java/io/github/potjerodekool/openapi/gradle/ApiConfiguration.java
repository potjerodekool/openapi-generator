package io.github.potjerodekool.openapi.gradle;

public class ApiConfiguration {

    private String openApiFile;
    private String basePackageName;
    private boolean generateModels = true;
    private boolean generateApiDefinitions = false;
    private boolean generateApiImplementations = false;

    public ApiConfiguration() {
    }

    public ApiConfiguration(final String openApiFile,
                            final String basePackageName,
                            final boolean generateModels,
                            final boolean generateApiDefinitions,
                            final boolean generateApiImplementations) {
        this.openApiFile = openApiFile;
        this.basePackageName = basePackageName;
        this.generateModels = generateModels;
        this.generateApiDefinitions = generateApiDefinitions;
        this.generateApiImplementations = generateApiImplementations;
    }

    public String getOpenApiFile() {
        return openApiFile;
    }

    public ApiConfiguration setOpenApiFile(final String openApiFile) {
        this.openApiFile = openApiFile;
        return this;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public ApiConfiguration setBasePackageName(final String basePackageName) {
        this.basePackageName = basePackageName;
        return this;
    }

    public boolean isGenerateModels() {
        return generateModels;
    }

    public ApiConfiguration setGenerateModels(final Boolean generateModels) {
        this.generateModels = generateModels;
        return this;
    }

    public boolean isGenerateApiDefinitions() {
        return generateApiDefinitions;
    }

    public ApiConfiguration setGenerateApiDefinitions(final Boolean generateApiDefinitions) {
        this.generateApiDefinitions = generateApiDefinitions;
        return this;
    }

    public boolean isGenerateApiImplementations() {
        return generateApiImplementations;
    }

    public void setGenerateApiImplementations(final Boolean generateApiImplementations) {
        this.generateApiImplementations = generateApiImplementations;
    }
}
