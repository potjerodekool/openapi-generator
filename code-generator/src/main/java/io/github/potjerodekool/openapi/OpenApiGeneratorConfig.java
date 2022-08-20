package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public interface OpenApiGeneratorConfig {

    static Builder createBuilder(final File apiFile,
                                 final File outputDir,
                                 final String configPackageName) {
        return new Builder(apiFile, outputDir, configPackageName);
    }

    Builder toBuilder();

    String FEATURE_JAKARTA_SERVLET = "feature.jakarta.servlet";

    String FEATURE_JAKARTA_VALIDATION = "feature.jakarta.validation";

    String FEATURE_CHECKER = "checkerFramework";

    File getApiFile();

    File getSchemasDir();

    File getPathsDir();

    File getOutputDir();

    String getConfigPackageName();

    boolean isGenerateApiDefinitions();

    boolean isGenerateModels();

    @Nullable Boolean getFeatureValue(String feature);

    default boolean isFeatureEnabled(final String feature) {
        return Boolean.TRUE.equals(getFeatureValue(feature));
    }

    class Builder {

        private final File apiFile;
        private final File outputDir;
        private final String configPackageName;

        private boolean generateApiDefinitions = true;

        private boolean generateModels = true;

        private final Map<String, Boolean> features;

        public Builder(final File apiFile,
                       final File outputDir,
                       final String configPackageName) {
            this(apiFile, outputDir, configPackageName, new HashMap<>());
        }

        public Builder(final File apiFile,
                       final File outputDir,
                       final String configPackageName,
                       final Map<String, Boolean> features) {
            this.apiFile = apiFile;
            this.outputDir = outputDir;
            this.configPackageName = configPackageName;
            this.features = features;
        }

        public void generateApiDefinitions(final boolean generateApiDefinitions) {
            this.generateApiDefinitions = generateApiDefinitions;
        }

        public void generateModels(final boolean generateModels) {
            this.generateModels = generateModels;
        }

        public void featureValue(final String feature, final Boolean value) {
            this.features.put(feature, value);
        }

        public OpenApiGeneratorConfig build() {
            final var config = new OpenApiGeneratorConfigImpl(
                    apiFile,
                    outputDir,
                    configPackageName
            );
            config.setGenerateApiDefinitions(generateApiDefinitions);
            config.setGenerateModels(generateModels);
            this.features.forEach(config::setFeatureValue);
            return config;
        }
    }

}
