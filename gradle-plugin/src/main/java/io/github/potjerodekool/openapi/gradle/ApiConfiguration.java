package io.github.potjerodekool.openapi.gradle;

public class ApiConfiguration {

    private String openApiFile;
    private String basePackageName;
    private Boolean generateModels = true;
    private Boolean generateApiDefinitions = false;
    private Boolean generateApiImplementations = false;

    public ApiConfiguration() {
    }

    public ApiConfiguration(final String openApiFile,
                            final String basePackageName,
                            final Boolean generateModels,
                            final Boolean generateApiDefinitions,
                            final Boolean generateApiImplementations) {
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

    public Boolean getGenerateModels() {
        return generateModels;
    }

    public ApiConfiguration setGenerateModels(final Boolean generateModels) {
        this.generateModels = generateModels;
        return this;
    }

    public Boolean generateApiDefinitions() {
        return generateApiDefinitions;
    }

    public ApiConfiguration setGenerateApiDefinitions(final Boolean generateApiDefinitions) {
        this.generateApiDefinitions = generateApiDefinitions;
        return this;
    }

    public Boolean getGenerateApiDefinitions() {
        return generateApiDefinitions;
    }

    public void setGenerateApiImplementations(final Boolean generateApiImplementations) {
        this.generateApiImplementations = generateApiImplementations;
    }
}
