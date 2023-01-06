package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiProperty(OpenApiType type,
                              boolean required,
                              @Nullable Boolean readOnly,
                              @Nullable Boolean writeOnly,
                              @Nullable String description,
                              Constraints constraints) {

    public OpenApiProperty(OpenApiType type,
                           boolean required,
                           @Nullable Boolean readOnly,
                           @Nullable Boolean writeOnly,
                           @Nullable String description) {
        this(
                type,
                required,
                readOnly,
                writeOnly,
                description,
                new Constraints()
        );
    }
}