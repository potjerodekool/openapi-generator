package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.ParameterLocation;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiParameter(ParameterLocation in,
                               String name,
                               Boolean required,
                               Boolean allowEmptyValue,
                               OpenApiType type,
                               @Nullable String defaultValue,
                               @Nullable String description) {
}
