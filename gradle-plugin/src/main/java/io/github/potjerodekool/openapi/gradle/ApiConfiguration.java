package io.github.potjerodekool.openapi.gradle;

public class ApiConfiguration {

    private String openApiFile;
    private String basePackageName;
    private Boolean generateModels = true;
    private Boolean generateDefinitions = true;

    public ApiConfiguration() {
    }

    public ApiConfiguration(final String openApiFile,
                            final String basePackageName,
                            final Boolean generateModels,
                            final Boolean generateDefinitions) {
        this.openApiFile = openApiFile;
        this.basePackageName = basePackageName;
        this.generateModels = generateModels;
        this.generateDefinitions = generateDefinitions;
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
        return generateDefinitions;
    }

    public ApiConfiguration setGenerateDefinitions(final Boolean generateDefinitions) {
        this.generateDefinitions = generateDefinitions;
        return this;
    }
}
