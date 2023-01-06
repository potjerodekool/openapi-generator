package io.github.potjerodekool.openapi;

import java.util.Map;

public record GeneratorConfig(Language language,
                              String basePackageName,
                              Map<String, Boolean> features) {

    public String configPackageName() {
        return basePackageName + ".config";
    }

    public boolean isFeatureEnabled(final String feature) {
        return Boolean.TRUE.equals(features.get(feature));
    }

}
