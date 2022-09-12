package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.ConfigType;
import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OpenApiGeneratorConfigImpl implements OpenApiGeneratorConfig {

    private final File apiFile;
    private final File schemasDir;
    private final File pathsDir;
    private final File outputDir;
    private final @Nullable String configPackageName;
    private final @Nullable String modelPackageName;
    private final Language language;
    private final ConfigType configType;

    private boolean generateApiDefinitions = true;
    private boolean generateModels = true;

    private final Map<String, Boolean> features = new HashMap<>();

    public OpenApiGeneratorConfigImpl(final File apiFile,
                                      final File outputDir,
                                      final @Nullable String configPackageName,
                                      final @Nullable String modelPackageName,
                                      final Language language,
                                      final ConfigType configType) {
        this.apiFile = apiFile;
        final var parentFile = apiFile.getParentFile();
        this.schemasDir = new File(parentFile, "schemas");
        this.pathsDir = new File(parentFile, "paths");
        this.outputDir = outputDir;
        this.configPackageName = configPackageName;
        this.modelPackageName = modelPackageName;
        this.language = language;
        this.configType = configType;
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
    public @Nullable String getConfigPackageName() {
        return configPackageName;
    }

    @Override
    public @Nullable String getModelPackageName() {
        return modelPackageName;
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

    public void setFeatureValue(final String feature,
                                final Boolean value) {
        features.put(feature, value);
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public Builder toBuilder() {
        final var builder = new Builder(
                apiFile,
                outputDir,
                configPackageName,
                modelPackageName,
                new HashMap<>(features),
                language
        );
        builder.generateApiDefinitions(generateApiDefinitions);
        builder.generateModels(generateModels);
        return builder;
    }

    @Override
    public ConfigType getConfigType() {
        return configType;
    }
}
