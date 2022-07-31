package com.github.potjerodekool.openapi.generate.api;

import com.github.potjerodekool.openapi.tree.OpenApiResponse;
import com.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

public final class ApiCodeGeneratorUtils {

    private ApiCodeGeneratorUtils() {
    }

    public static Optional<OpenApiResponse> find2XXResponse(final Map<String, OpenApiResponse> responses) {
        return responses.entrySet().stream()
                .filter(it -> it.getKey().length() == 3 && it.getKey().startsWith("2"))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public static Optional<OpenApiResponse> findDefaultResponse(final Map<String, OpenApiResponse> responses) {
        return responses.entrySet().stream()
                .filter(it -> it.getKey().equals("default"))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public static boolean hasCreateResponseCode(final Map<String, OpenApiResponse> responses) {
        return responses.entrySet().stream()
                .anyMatch(it -> it.getKey().equals("201"));
    }

    public static @Nullable OpenApiType findJsonMediaType(final Map<String, OpenApiType> contentMediaType) {
        return contentMediaType.get("application/json");
    }
}
