package com.github.potjerodekool.openapi.tree;

import com.github.potjerodekool.openapi.ParameterLocation;
import com.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiParameter(ParameterLocation in,
                               String name,
                               Boolean required,
                               Boolean allowEmptyValue,
                               OpenApiType type,
                               @Nullable String defaultValue,
                               @Nullable String description) {
}
