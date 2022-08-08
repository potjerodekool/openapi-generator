package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public record OpenApiProperty(OpenApiType type,
                              boolean required,
                              @Nullable Boolean readOnly,
                              @Nullable Boolean writeOnly,
                              @Nullable Number minimum,
                              @Nullable Boolean exclusiveMinimum,
                              @Nullable Number maximum,
                              @Nullable Boolean exclusiveMaximum,
                              @Nullable Integer minLength,
                              @Nullable Integer maxLength,
                              @Nullable String pattern,
                              @Nullable Integer minItems,
                              @Nullable Integer maxItems,
                              @Nullable Boolean uniqueItems,
                              @Nullable List<Object> enums) {

    public OpenApiProperty(OpenApiType type,
                           boolean required,
                           @Nullable Boolean readOnly,
                           @Nullable Boolean writeOnly) {
        this(
                type,
                required,
                readOnly,
                writeOnly,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}