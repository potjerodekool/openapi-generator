package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public interface OpenApiGeneratorConfig {

    String FEATURE_JAKARTA_SERVLET = "feature.jakarta.servlet";

    String FEATURE_JAKARTA_VALIDATION = "feature.jakarta.validation";

    String FEATURE_CHECKER = "checkerFramework";

    static Builder createBuilder(final File apiFile,
                                 final File outputDir,
                                 final @Nullable String configPackageName,
                                 final @Nullable String modelPackageName) {
        return new Builder(apiFile, outputDir, configPackageName, modelPackageName, Language.JAVA);
    }

    File getApiFile();

    Language getLanguage();

    Builder toBuilder();

    File getSchemasDir();

    File getPathsDir();

    File getOutputDir();

    @Nullable String getConfigPackageName();

    @Nullable String getModelPackageName();

    boolean isGenerateApiDefinitions();

    boolean isGenerateModels();

    @Nullable Boolean getFeatureValue(String feature);

    default boolean isFeatureEnabled(final String feature) {
        return Boolean.TRUE.equals(getFeatureValue(feature));
    }

    ConfigType getConfigType();

    class Builder {

        private final File apiFile;
        private final File outputDir;
        private final @Nullable String configPackageName;
        private final @Nullable String modelPackageName;
        private boolean generateApiDefinitions = true;
        private boolean generateModels = true;
        private final Map<String, Boolean> features;
        private Language language;

        public Builder(final File apiFile,
                       final File outputDir,
                       final @Nullable String configPackageName,
                       final @Nullable String modelPackageName,
                       final Language language) {
            this(apiFile, outputDir, configPackageName, modelPackageName, new HashMap<>(), language);
        }

        public Builder(final File apiFile,
                       final File outputDir,
                       final @Nullable String configPackageName,
                       final @Nullable String modelPackageName,
                       final Map<String, Boolean> features,
                       final Language language) {
            this.apiFile = apiFile;
            this.outputDir = outputDir;
            this.configPackageName = configPackageName;
            this.modelPackageName = modelPackageName;
            this.features = features;
            this.language = language;
        }

        public Builder generateApiDefinitions(final boolean generateApiDefinitions) {
            this.generateApiDefinitions = generateApiDefinitions;
            return this;
        }

        public Builder generateModels(final boolean generateModels) {
            this.generateModels = generateModels;
            return this;
        }

        public Builder featureValue(final String feature, final Boolean value) {
            this.features.put(feature, value);
            return this;
        }

        public Builder language(final Language language) {
            this.language = language;
            return this;
        }

        public OpenApiGeneratorConfig build(final ConfigType configType) {
            final var config = new OpenApiGeneratorConfigImpl(
                    apiFile,
                    outputDir,
                    configPackageName,
                    modelPackageName,
                    language,
                    configType
            );
            config.setGenerateApiDefinitions(generateApiDefinitions);
            config.setGenerateModels(generateModels);
            this.features.forEach(config::setFeatureValue);
            return config;
        }
    }

}

