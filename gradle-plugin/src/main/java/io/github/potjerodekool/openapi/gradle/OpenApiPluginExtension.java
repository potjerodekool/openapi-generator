package io.github.potjerodekool.openapi.gradle;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.Map;

abstract public class OpenApiPluginExtension {

    abstract public Property<String> getBasePackageName();
    abstract public Property<Boolean> getJakarta();
    abstract public Property<Boolean> getChecker();
    abstract public Property<String> getLanguage();

    abstract public ListProperty<ApiConfiguration> getApis();

    public ApiConfiguration apiConfig(final Map<String, Object> map) {
        final String openApiFile = (String) map.get("openApiFile");
        final String basePackageName = (String) map.get("basePackageName");
        final Boolean generateModels = (Boolean) map.get("generateModels");
        final Boolean generateApiDefinitions = (Boolean) map.get("generateApiDefinitions");
        return new ApiConfiguration(openApiFile, basePackageName, generateModels, generateApiDefinitions);
    }


}