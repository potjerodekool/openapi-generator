package io.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;

public interface OpenApiGeneratorConfig {

    String FEATURE_JAKARTA_SERVLET = "feature.jakarta.servlet";

    String FEATURE_JAKARTA_VALIDATION = "feature.jakarta.validation";

    String FEATURE_HIBERNATE_VALIDATION = "feature.hibernate.validation";

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

    void setFeatureValue(String feature, Boolean value);

}
