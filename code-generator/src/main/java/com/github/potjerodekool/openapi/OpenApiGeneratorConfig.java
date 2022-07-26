package com.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;

public class OpenApiGeneratorConfig {

    private final File apiFile;
    private final File schemasDir;
    private final File pathsDir;
    private final File outputDir;
    private boolean addCheckerAnnotations;
    private @Nullable String configPackageName;
    private boolean generateApiDefinitions = true;
    private boolean generateApiImplementations = true;
    private boolean generateModels = true;
    private boolean useJakartaServlet = false;
    private boolean useJakartaValidation = false;

    public OpenApiGeneratorConfig(final File apiFile,
                                  final File outputDir) {
        this.apiFile = apiFile;
        final var parentFile = apiFile.getParentFile();
        schemasDir = new File(parentFile, "schemas");
        pathsDir = new File(parentFile, "paths");
        this.outputDir = outputDir;
    }

    public File getApiFile() {
        return apiFile;
    }

    public File getSchemasDir() { return schemasDir; }

    public File getPathsDir() {
        return pathsDir;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setAddCheckerAnnotations(final boolean addCheckerAnnotations) {
        this.addCheckerAnnotations = addCheckerAnnotations;
    }

    public boolean isAddCheckerAnnotations() {
        return addCheckerAnnotations;
    }

    public @Nullable String getConfigPackageName() {
        return configPackageName;
    }

    public void setConfigPackageName(final String configPackageName) {
        this.configPackageName = configPackageName;
    }

    public boolean isGenerateApiDefinitions() {
        return generateApiDefinitions;
    }

    public void setGenerateApiDefinitions(final boolean generateApiDefinitions) {
        this.generateApiDefinitions = generateApiDefinitions;
    }

    public boolean isGenerateApiImplementations() {
        return generateApiImplementations;
    }

    public void setGenerateApiImplementations(final boolean generateApiImplementations) {
        this.generateApiImplementations = generateApiImplementations;
    }

    public boolean isGenerateModels() {
        return generateModels;
    }

    public void setGenerateModels(final boolean generateModels) {
        this.generateModels = generateModels;
    }

    public boolean isUseJakartaServlet() {
        return useJakartaServlet;
    }

    public void setUseJakartaServlet(final boolean useJakartaServlet) {
        this.useJakartaServlet = useJakartaServlet;
    }

    public boolean isUseJakartaValidation() {
        return useJakartaValidation;
    }

    public void setUseJakartaValidation(final boolean useJakartaValidation) {
        this.useJakartaValidation = useJakartaValidation;
    }
}
