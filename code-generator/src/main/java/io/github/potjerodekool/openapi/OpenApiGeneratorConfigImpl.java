package io.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OpenApiGeneratorConfigImpl implements OpenApiGeneratorConfig {

    private final File apiFile;
    private final File schemasDir;
    private final File pathsDir;
    private final File outputDir;
    private final String configPackageName;

    private boolean generateApiDefinitions = true;
    private boolean generateModels = true;

    private final Map<String, Boolean> features = new HashMap<>();

    public OpenApiGeneratorConfigImpl(final File apiFile,
                                      final File outputDir,
                                      final String configPackageName) {
        this.apiFile = apiFile;
        final var parentFile = apiFile.getParentFile();
        schemasDir = new File(parentFile, "schemas");
        pathsDir = new File(parentFile, "paths");
        this.outputDir = outputDir;
        this.configPackageName = configPackageName;
    }

    @Override
    public File getApiFile() {
        return apiFile;
    }

    @Override
    public File getSchemasDir() { return schemasDir; }

    @Override
    public File getPathsDir() {
        return pathsDir;
    }

    @Override
    public File getOutputDir() {
        return outputDir;
    }


    @Override
    public String getConfigPackageName() {
        return configPackageName;
    }

    @Override
    public boolean isGenerateApiDefinitions() {
        return generateApiDefinitions;
    }

    public void setGenerateApiDefinitions(final boolean generateApiDefinitions) {
        this.generateApiDefinitions = generateApiDefinitions;
    }

    @Override
    public boolean isGenerateModels() {
        return generateModels;
    }

    public void setGenerateModels(final boolean generateModels) {
        this.generateModels = generateModels;
    }

    @Override
    public @Nullable Boolean getFeatureValue(final String feature) {
        return features.get(feature);
    }

    @Override
    public void setFeatureValue(final String feature,
                                final Boolean value) {
        features.put(feature, value);
    }
}
