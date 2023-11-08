package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.ParameterLocation;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiParameter(ParameterLocation in,
                               OpenApiSchema<?> type,
                               String name,
                               @Nullable String description,
                               @Nullable Boolean required,
                               @Nullable Boolean allowEmptyValue,
                               @Nullable Boolean explode,
                               @Nullable Boolean nullable,
                               @Nullable String example) {
}
