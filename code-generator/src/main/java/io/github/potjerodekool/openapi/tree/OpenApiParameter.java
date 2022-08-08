package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.ParameterLocation;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiParameter(ParameterLocation in,
                               OpenApiType type,
                               String name,
                               @Nullable String description,
                               @Nullable Boolean required,
                               @Nullable Boolean allowEmptyValue,
                               @Nullable Boolean explode,
                               @Nullable String example) {
}
